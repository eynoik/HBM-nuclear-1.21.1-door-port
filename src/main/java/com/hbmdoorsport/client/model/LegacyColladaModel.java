package com.hbmdoorsport.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modern rendering endpoint for HBM's old ColladaLoader.
 * It intentionally follows the 1.12 parser's assumptions: position/normal/UV triplets,
 * Blender transform matrices, per-object keyframes and quaternion slerp.
 */
public final class LegacyColladaModel {
    private final RootNode root;
    private final Map<String, Geometry> geometry;
    private final Map<String, KeyTransform[]> animation;
    private final int keyFrames;

    private LegacyColladaModel(RootNode root, Map<String, Geometry> geometry, Map<String, KeyTransform[]> animation, int keyFrames) {
        this.root=root; this.geometry=geometry; this.animation=animation; this.keyFrames=keyFrames;
    }

    public static LegacyColladaModel load(ResourceLocation model, ResourceLocation anim, boolean flipV) {
        try {
            Document modelDoc=read(model); Document animDoc=model.equals(anim)?modelDoc:read(anim);
            RootNode root=parseStructure(modelDoc);
            Map<String,Geometry> geometry=parseGeometry(modelDoc,flipV);
            ParsedAnimation pa=parseAnimation(animDoc);
            return new LegacyColladaModel(root,geometry,pa.map,pa.frames);
        } catch(Exception e) { throw new IllegalStateException("Failed to load legacy COLLADA " + model + " / " + anim,e); }
    }

    private static Document read(ResourceLocation loc) throws Exception {
        try(InputStream in=Minecraft.getInstance().getResourceManager().getResource(loc).orElseThrow().open()) {
            DocumentBuilderFactory f=DocumentBuilderFactory.newInstance(); f.setNamespaceAware(false);
            return f.newDocumentBuilder().parse(in);
        }
    }

    public void render(PoseStack ps,VertexConsumer out,int light,int overlay,float progress) {
        float mapped=keyFrames<=1?0:clamp(progress,0,1)*(keyFrames-1);
        int first=(int)Math.floor(mapped); int next=Math.min(keyFrames-1,first+1); float inter=mapped-first;
        for(ModelNode n:root.children) renderNode(n,ps,out,light,overlay,first,next,inter);
    }

    private void renderNode(ModelNode n,PoseStack ps,VertexConsumer out,int light,int overlay,int first,int next,float inter) {
        ps.pushPose();
        boolean hidden=false;
        KeyTransform[] arr=animation.get(n.name);
        if(arr!=null&&arr.length>0){
            int a=Math.min(first,arr.length-1),b=Math.min(next,arr.length-1);
            hidden=arr[a].hidden;
            arr[a].applyInterpolated(arr[b],inter,ps);
        } else if(n.base!=null) {
            // HBM 1.12 multiplied static node matrices directly. Do not decompose them into
            // TRS: after flipMatrix that loses the original translation/pivot basis and shifts
            // nested pieces (especially Transition Seal and Silo Hatch).
            ps.mulPose(n.base);
        }
        Geometry g=geometry.get(n.geometry);
        if(g!=null&&!hidden)g.render(ps.last(),out,light,overlay);
        for(ModelNode c:n.children)renderNode(c,ps,out,light,overlay,first,next,inter);
        ps.popPose();
    }

    private static RootNode parseStructure(Document doc) {
        Element lib=(Element)doc.getElementsByTagName("library_visual_scenes").item(0);
        Element scene=firstElement(lib); RootNode root=new RootNode();
        for(Element e:children(scene)) if(e.getElementsByTagName("instance_geometry").getLength()>0) root.children.add(parseNode(e));
        return root;
    }

    private static ModelNode parseNode(Element e) {
        ModelNode n=new ModelNode(); n.name=e.getAttribute("name");
        for(Element c:children(e)) {
            if("transform".equals(c.getAttribute("sid"))) {
                float[] raw=flipMatrix(parseFloats(c.getTextContent()));
                if(raw.length==16)n.base=new Matrix4f().set(raw);
            }
            else if("instance_geometry".equals(c.getTagName())) {String url=c.getAttribute("url");n.geometry=url.startsWith("#")?url.substring(1):url;}
            else if(c.getElementsByTagName("instance_geometry").getLength()>0)n.children.add(parseNode(c));
        }
        return n;
    }

    private static Map<String,Geometry> parseGeometry(Document doc,boolean flipV) {
        Map<String,Geometry> result=new HashMap<>(); Element lib=(Element)doc.getElementsByTagName("library_geometries").item(0);
        for(Element geo:directByName(lib,"geometry")) {
            String name=geo.getAttribute("id"); Element mesh=directByName(geo,"mesh").get(0);
            float[] pos={},norm={},uv={}; List<Integer> indices=new ArrayList<>();
            for(Element s:children(mesh)) {
                String id=s.getAttribute("id");
                if(id.endsWith("mesh-positions"))pos=parseFloatArrayElement(s);
                else if(id.endsWith("mesh-normals"))norm=parseFloatArrayElement(s);
                else if(id.endsWith("mesh-map-0"))uv=parseFloatArrayElement(s);
                else if("triangles".equals(s.getTagName()))for(int i:parseInts(((Element)s.getElementsByTagName("p").item(0)).getTextContent()))indices.add(i);
            }
            if(pos.length==0)continue;
            List<Vertex> verts=new ArrayList<>();
            for(int i=0;i+2<indices.size();i+=3){
                int pi=indices.get(i),ni=indices.get(i+1),ti=indices.get(i+2);
                float u=uv.length>ti*2?uv[ti*2]:0, v=uv.length>ti*2+1?uv[ti*2+1]:0; if(flipV)v=1-v;
                float nx=norm.length>ni*3?norm[ni*3]:0,ny=norm.length>ni*3+1?norm[ni*3+1]:1,nz=norm.length>ni*3+2?norm[ni*3+2]:0;
                verts.add(new Vertex(pos[pi*3],pos[pi*3+1],pos[pi*3+2],u,v,nx,ny,nz));
            }
            result.put(name,new Geometry(verts));
        }
        return result;
    }

    private static ParsedAnimation parseAnimation(Document doc) {
        Map<String,KeyTransform[]> map=new HashMap<>(); int frames=1;
        Element lib=(Element)doc.getElementsByTagName("library_animations").item(0); if(lib==null)return new ParsedAnimation(map,frames);
        for(Element a:children(lib)) {
            if(!"animation".equals(a.getTagName()))continue; String name=a.getAttribute("name"); KeyTransform[] transforms=null;
            for(Element child:children(a)) {
                String id=child.getAttribute("id");
                if(id.endsWith("transform"))transforms=parseTransforms(child);
                else if(id.endsWith("hide_viewport")&&transforms!=null)applyHidden(transforms,child);
            }
            if(transforms!=null){map.put(name,transforms);frames=Math.max(frames,transforms.length);}
        }
        return new ParsedAnimation(map,frames);
    }

    private static KeyTransform[] parseTransforms(Element anim) {
        String output=outputLocation(anim); if(output==null)return null;
        for(Element e:children(anim))if(output.equals(e.getAttribute("id"))){
            float[] f=parseFloatArrayElement(e); KeyTransform[] out=new KeyTransform[f.length/16];
            for(int i=0;i<out.length;i++){float[] m=new float[16];System.arraycopy(f,i*16,m,0,16);out[i]=new KeyTransform(m);}
            return out;
        }
        return null;
    }

    private static void applyHidden(KeyTransform[] t,Element anim) {
        String output=outputLocation(anim); if(output==null)return;
        for(Element e:children(anim))if(output.equals(e.getAttribute("id"))){float[] f=parseFloatArrayElement(e);for(int i=0;i<t.length&&i<f.length;i++)t[i].hidden=f[i]>0;}
    }

    private static String outputLocation(Element anim) {
        NodeList samplers=anim.getElementsByTagName("sampler"); if(samplers.getLength()==0)return null;
        for(Element e:children((Element)samplers.item(0)))if("OUTPUT".equals(e.getAttribute("semantic"))){String s=e.getAttribute("source");return s.startsWith("#")?s.substring(1):s;}
        return null;
    }

    private static float[] parseFloatArrayElement(Element e){Node n=e.getElementsByTagName("float_array").item(0);return n==null?new float[0]:parseFloats(n.getTextContent());}
    private static float[] parseFloats(String s){String[] p=s.trim().split("\\s+");if(p.length==1&&p[0].isEmpty())return new float[0];float[] r=new float[p.length];for(int i=0;i<p.length;i++)r[i]=Float.parseFloat(p[i]);return r;}
    private static int[] parseInts(String s){String[] p=s.trim().split("\\s+");int[] r=new int[p.length];for(int i=0;i<p.length;i++)r[i]=Integer.parseInt(p[i]);return r;}
    private static float[] flipMatrix(float[] f){if(f.length!=16)return f;return new float[]{f[0],f[4],f[8],f[12],f[1],f[5],f[9],f[13],f[2],f[6],f[10],f[14],f[3],f[7],f[11],f[15]};}
    private static Element firstElement(Element e){if(e==null)return null;for(Element x:children(e))return x;return null;}
    private static List<Element> children(Element e){List<Element> r=new ArrayList<>();if(e==null)return r;NodeList n=e.getChildNodes();for(int i=0;i<n.getLength();i++)if(n.item(i).getNodeType()==Node.ELEMENT_NODE)r.add((Element)n.item(i));return r;}
    private static List<Element> directByName(Element e,String name){List<Element> r=new ArrayList<>();for(Element x:children(e))if(name.equals(x.getTagName()))r.add(x);return r;}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}

    private static final class RootNode { final List<ModelNode> children=new ArrayList<>(); }
    private static final class ModelNode { String name="",geometry=""; Matrix4f base; final List<ModelNode> children=new ArrayList<>(); }
    private record ParsedAnimation(Map<String,KeyTransform[]> map,int frames) { }

    private static final class Geometry {
        final List<Vertex> verts; Geometry(List<Vertex> verts){this.verts=verts;}
        void render(PoseStack.Pose pose,VertexConsumer out,int light,int overlay){
            // Entity RenderTypes consume QUADS. The legacy Collada loader produces TRIANGLES.
            // Emit every triangle as a degenerate quad (A,B,C,C), otherwise Minecraft
            // stitches vertex 4 from the next triangle into the previous face.
            for(int i=0;i+2<verts.size();i+=3){
                Vertex a=verts.get(i), b=verts.get(i+1), c=verts.get(i+2);
                emit(pose,out,light,overlay,a); emit(pose,out,light,overlay,b);
                emit(pose,out,light,overlay,c); emit(pose,out,light,overlay,c);
            }
        }
        private static void emit(PoseStack.Pose pose,VertexConsumer out,int light,int overlay,Vertex v){
            out.addVertex(pose,v.x,v.y,v.z).setColor(255,255,255,255).setUv(v.u,v.v).setOverlay(overlay).setLight(light).setNormal(pose,v.nx,v.ny,v.nz);
        }
    }
    private record Vertex(float x,float y,float z,float u,float v,float nx,float ny,float nz) { }

    /** Same TRS extraction and quaternion interpolation strategy as HBM 1.12 Transform. */
    private static final class KeyTransform {
        final float tx,ty,tz,sx,sy,sz,qx,qy,qz,qw; boolean hidden;
        KeyTransform(float[] matrix){
            float[] m=matrix.clone();
            sx=len(m[0],m[1],m[2]); sy=len(m[4],m[5],m[6]); sz=len(m[8],m[9],m[10]);
            float xs=sx==0?1:sx,ys=sy==0?1:sy,zs=sz==0?1:sz;
            m[0]/=xs;m[1]/=xs;m[2]/=xs;m[4]/=ys;m[5]/=ys;m[6]/=ys;m[8]/=zs;m[9]/=zs;m[10]/=zs;
            float[] q=quatFromMatrix(m);qx=q[0];qy=q[1];qz=q[2];qw=q[3];tx=m[3];ty=m[7];tz=m[11];
        }
        void applyInterpolated(KeyTransform b,float t,PoseStack ps){float[] q=slerp(qx,qy,qz,qw,b.qx,b.qy,b.qz,b.qw,t);applyValues(ps,mix(tx,b.tx,t),mix(ty,b.ty,t),mix(tz,b.tz,t),mix(sx,b.sx,t),mix(sy,b.sy,t),mix(sz,b.sz,t),q[0],q[1],q[2],q[3]);}
        private static void applyValues(PoseStack ps,float tx,float ty,float tz,float sx,float sy,float sz,float qx,float qy,float qz,float qw){ps.translate(tx,ty,tz);ps.mulPose(new Quaternionf(qx,qy,qz,qw));ps.scale(sx,sy,sz);}
        private static float len(float a,float b,float c){return (float)Math.sqrt(a*a+b*b+c*c);}
        private static float mix(float a,float b,float t){return a+(b-a)*t;}
        private static float[] quatFromMatrix(float[] m){
            float m00=m[0],m01=m[1],m02=m[2],m10=m[4],m11=m[5],m12=m[6],m20=m[8],m21=m[9],m22=m[10]; float x,y,z,w; float tr=m00+m11+m22;
            if(tr>0){float s=(float)Math.sqrt(tr+1.0f)*2;w=.25f*s;x=(m21-m12)/s;y=(m02-m20)/s;z=(m10-m01)/s;}
            else if(m00>m11&&m00>m22){float s=(float)Math.sqrt(1+m00-m11-m22)*2;w=(m21-m12)/s;x=.25f*s;y=(m01+m10)/s;z=(m02+m20)/s;}
            else if(m11>m22){float s=(float)Math.sqrt(1+m11-m00-m22)*2;w=(m02-m20)/s;x=(m01+m10)/s;y=.25f*s;z=(m12+m21)/s;}
            else{float s=(float)Math.sqrt(1+m22-m00-m11)*2;w=(m10-m01)/s;x=(m02+m20)/s;y=(m12+m21)/s;z=.25f*s;}
            float l=len4(x,y,z,w);return l==0?new float[]{0,0,0,1}:new float[]{x/l,y/l,z/l,w/l};
        }
        private static float len4(float a,float b,float c,float d){return (float)Math.sqrt(a*a+b*b+c*c+d*d);}
        private static float[] slerp(float ax,float ay,float az,float aw,float bx,float by,float bz,float bw,float t){
            float dot=ax*bx+ay*by+az*bz+aw*bw;if(dot<0){bx=-bx;by=-by;bz=-bz;bw=-bw;dot=-dot;}
            if(dot>0.9999999f){float x=ax+t*bx,y=ay+t*by,z=az+t*bz,w=aw+t*bw,l=len4(x,y,z,w);return new float[]{x/l,y/l,z/l,w/l};}
            double theta0=Math.acos(Math.max(-1,Math.min(1,dot))),theta=theta0*t,sin=Math.sin(theta),sin0=Math.sin(theta0);float s0=(float)(Math.cos(theta)-dot*sin/sin0),s1=(float)(sin/sin0);
            return new float[]{s0*ax+s1*bx,s0*ay+s1*by,s0*az+s1*bz,s0*aw+s1*bw};
        }
    }
}
