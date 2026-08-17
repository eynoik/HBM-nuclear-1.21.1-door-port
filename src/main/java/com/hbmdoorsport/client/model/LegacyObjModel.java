package com.hbmdoorsport.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tiny OBJ path preserving HBM object/group names and its Wavefront V flip. */
public final class LegacyObjModel {
    private final Map<String, List<Triangle>> groups;

    private LegacyObjModel(Map<String, List<Triangle>> groups) { this.groups = groups; }

    public static LegacyObjModel load(ResourceLocation location) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Minecraft.getInstance().getResourceManager().getResource(location).orElseThrow().open(), StandardCharsets.UTF_8))) {
            List<Vec> positions = new ArrayList<>();
            List<UV> uvs = new ArrayList<>();
            List<Vec> normals = new ArrayList<>();
            Map<String, List<Triangle>> groups = new HashMap<>();
            String group = "default";
            groups.put(group, new ArrayList<>());
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\s+");
                switch (p[0]) {
                    case "v" -> positions.add(new Vec(f(p[1]),f(p[2]),f(p[3])));
                    case "vt" -> uvs.add(new UV(f(p[1]),1.0F-f(p[2])));
                    case "vn" -> normals.add(new Vec(f(p[1]),f(p[2]),f(p[3])));
                    case "o", "g" -> { group = p.length > 1 ? p[1] : "default"; groups.computeIfAbsent(group,k->new ArrayList<>()); }
                    case "f" -> {
                        List<Vertex> face = new ArrayList<>(p.length-1);
                        for (int i=1;i<p.length;i++) face.add(parseRef(p[i],positions,uvs,normals));
                        for (int i=1;i+1<face.size();i++) groups.get(group).add(new Triangle(face.get(0),face.get(i),face.get(i+1)));
                    }
                    default -> { }
                }
            }
            return new LegacyObjModel(groups);
        } catch (IOException e) { throw new IllegalStateException("Failed to load legacy OBJ " + location, e); }
    }

    private static Vertex parseRef(String ref,List<Vec> pos,List<UV> uv,List<Vec> norm) {
        String[] idx=ref.split("/",-1);
        Vec p=pos.get(index(idx[0],pos.size()));
        UV t=idx.length>1&&!idx[1].isEmpty()?uv.get(index(idx[1],uv.size())):new UV(0,0);
        Vec n=idx.length>2&&!idx[2].isEmpty()?norm.get(index(idx[2],norm.size())):new Vec(0,1,0);
        return new Vertex(p.x,p.y,p.z,t.u,t.v,n.x,n.y,n.z);
    }
    private static int index(String raw,int size){int i=Integer.parseInt(raw);return i>0?i-1:size+i;}
    private static float f(String s){return Float.parseFloat(s);}

    public void renderAllRaw(PoseStack.Pose pose, VertexConsumer out, int light, int overlay) {
        for (String name : groups.keySet()) renderGroupRaw(name, pose, out, light, overlay);
    }

    public void renderGroupRaw(String name, PoseStack.Pose pose, VertexConsumer out, int light, int overlay) {
        renderGroupTranslatedClipped(name,0,0,0,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY,pose,out,light,overlay);
    }

    /** Compatibility method used by the round-airlock proof: exact +/-2.0001 Z clips. */
    public void renderGroup(String name,float dz,PoseStack.Pose pose,VertexConsumer out,int light,int overlay) {
        renderGroupTranslatedClipped(name,0,0,dz,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY,-2.0001F,2.0001F,pose,out,light,overlay);
    }

    public void renderGroupTranslatedClipped(String name,float dx,float dy,float dz,
                                             float minX,float maxX,float minY,float maxY,float minZ,float maxZ,
                                             PoseStack.Pose pose,VertexConsumer out,int light,int overlay) {
        List<Triangle> tris=groups.get(name); if(tris==null)return;
        for(Triangle tri:tris){
            List<Vertex> poly=new ArrayList<>(3);
            poly.add(tri.a.translate(dx,dy,dz)); poly.add(tri.b.translate(dx,dy,dz)); poly.add(tri.c.translate(dx,dy,dz));
            if(Float.isFinite(minX)) poly=clip(poly,0,minX,true); if(poly.size()<3)continue;
            if(Float.isFinite(maxX)) poly=clip(poly,0,maxX,false); if(poly.size()<3)continue;
            if(Float.isFinite(minY)) poly=clip(poly,1,minY,true); if(poly.size()<3)continue;
            if(Float.isFinite(maxY)) poly=clip(poly,1,maxY,false); if(poly.size()<3)continue;
            if(Float.isFinite(minZ)) poly=clip(poly,2,minZ,true); if(poly.size()<3)continue;
            if(Float.isFinite(maxZ)) poly=clip(poly,2,maxZ,false); if(poly.size()<3)continue;
            for(int i=1;i+1<poly.size();i++) emitTriangle(poly.get(0),poly.get(i),poly.get(i+1),pose,out,light,overlay);
        }
    }

    private static List<Vertex> clip(List<Vertex> in,int axis,float plane,boolean keepGreater){
        List<Vertex> out=new ArrayList<>(in.size()+1); if(in.isEmpty())return out;
        Vertex prev=in.get(in.size()-1); float pv=prev.axis(axis); boolean pin=keepGreater?pv>=plane:pv<=plane;
        for(Vertex cur:in){float cv=cur.axis(axis);boolean cin=keepGreater?cv>=plane:cv<=plane;
            if(cin!=pin){float t=(plane-pv)/(cv-pv);out.add(Vertex.lerp(prev,cur,t));}
            if(cin)out.add(cur);prev=cur;pv=cv;pin=cin;}
        return out;
    }

    private static void emitTriangle(Vertex a,Vertex b,Vertex c,PoseStack.Pose pose,VertexConsumer out,int light,int overlay){
        emit(a,pose,out,light,overlay);emit(b,pose,out,light,overlay);emit(c,pose,out,light,overlay);emit(c,pose,out,light,overlay);
    }
    private static void emit(Vertex v,PoseStack.Pose pose,VertexConsumer out,int light,int overlay){
        out.addVertex(pose,v.x,v.y,v.z).setColor(255,255,255,255).setUv(v.u,v.v).setOverlay(overlay).setLight(light).setNormal(pose,v.nx,v.ny,v.nz);
    }

    private record Vec(float x,float y,float z){}
    private record UV(float u,float v){}
    private record Triangle(Vertex a,Vertex b,Vertex c){}
    private record Vertex(float x,float y,float z,float u,float v,float nx,float ny,float nz){
        Vertex translate(float dx,float dy,float dz){return new Vertex(x+dx,y+dy,z+dz,u,v,nx,ny,nz);}
        float axis(int a){return a==0?x:a==1?y:z;}
        static Vertex lerp(Vertex a,Vertex b,float t){
            float nx=mix(a.nx,b.nx,t),ny=mix(a.ny,b.ny,t),nz=mix(a.nz,b.nz,t);float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
            if(len>1e-6F){nx/=len;ny/=len;nz/=len;}
            return new Vertex(mix(a.x,b.x,t),mix(a.y,b.y,t),mix(a.z,b.z,t),mix(a.u,b.u,t),mix(a.v,b.v,t),nx,ny,nz);
        }
        private static float mix(float a,float b,float t){return a+(b-a)*t;}
    }
}
