import java.awt.*;

public class Teapot {
    // A Quadrilateral, use to draw the teapot patches
    static class Quad {
        public int[] px;
        public int[] py;
        Color col;
        Quad next;
    }
    // size of draw order list array
    private final int ZSIZE=256;
    // rotation matrix
    private float[][] rmx=new float[3][3];
    // position on the screen
    private int positionx, positiony;
    // size of teapot
    private float size;
    // draw order is determined by an array of lists
    private Quad[] quad_lists=new Quad[ZSIZE];
    // light direction
    private Vec3f lightdir=new Vec3f(0.577f,-0.577f,0.577f);
    // colours
    private Vec3f ambient_colour=new Vec3f(20,20,20);
    private Vec3f material_colour;
    private Vec3f light_colour=new Vec3f(255,255,255);
    private float specularstrength=0.65f;
    // teapot rotation
    private Vec3f rotation;

    // make the rotation matrix from the rotation vector
    private void maketrotationmatrix(Vec3f rotation, float size) {
        float ca = (float) Math.cos(rotation.x);
        float cb = (float) Math.cos(rotation.y);
        float cc = (float) Math.cos(rotation.z);
        float sa = (float) Math.sin(rotation.x);
        float sb = (float) Math.sin(rotation.y);
        float sc = (float) Math.sin(rotation.z);
        rmx[0][0] = cc * cb * size;
        rmx[0][1] = (cc * sb * sa - sc * ca) * size;
        rmx[0][2] = (cc * sb * ca + sc * sa) * size;
        rmx[1][0] = sc * cb * size;
        rmx[1][1] = (sc * sb * sa + cc * ca) * size;
        rmx[1][2] = (sc * sb * ca - cc * sa) * size;
        rmx[2][0] = -sb * size;
        rmx[2][1] = cb * sa * size;
        rmx[2][2] = cb * ca * size;
    }
    // init the list of quads
    private void quad_init() {
        for(int i=0;i<ZSIZE;i++)
            quad_lists[i]=null;
    }

    // add a quad to one of the lists and work out what colour to draw it.
    void add_quad(Vec3f p0, Vec3f p1, Vec3f p2, Vec3f p3) {
        // get the normal vector
        Vec3f normal=p2.sub(p0).cross(p3.sub(p0));
        // don't draw it if it's facing away from us.
        if(normal.z<=0) return;
        normal.normalise();
        float dp=normal.dot(lightdir);
        float diff=clamp(dp, 1.0f);
        // diffuse lighting
        Vec3f diffuse=material_colour.mul(diff);
        // specular lighting
        float spec=clamp(2.0f*dp*normal.z-lightdir.z, 2);
        spec=spec*spec;
        spec=spec*spec;
        spec=spec*spec;
        spec=specularstrength*spec;
        Vec3f specular=light_colour.mul(spec);
        Vec3f res=ambient_colour.add(diffuse.add(specular));
        Vec3f colour=new Vec3f(clamp(res.x, 255),clamp(res.y, 255),clamp(res.z, 255));
        // use average z value for the quad as the list index.
        // so they are drawn with the closest last
        float z=Math.max(Math.max(Math.max(p0.z,p1.z),p2.z),p3.z);
        int zindex=clamp((int)z+ZSIZE/2,0,ZSIZE-1);
       // int zindex=clamp((int)((p0.z+p1.z+p2.z+p3.z)/4)+ZSIZE/2,0,ZSIZE-1);
        Quad q= new Quad();
        q.px= new int[]{(int) (p0.x + 0.5f), (int) (p1.x + 0.5f), (int) (p2.x + 0.5f), (int) (p3.x + 0.5f)};
        q.py= new int[]{(int) (p0.y + 0.5f), (int) (p1.y + 0.5f), (int) (p2.y + 0.5f), (int) (p3.y + 0.5f)};
        q.col=new Color((int)colour.x,(int)colour.y,(int)colour.z);
        q.next=quad_lists[zindex];
        quad_lists[zindex]=q;
    }

    // apply the rotation matrix to a vector stored in the array of teapot vertices
    Vec3f vrotate(float[] v) {
        float v2=v[2]-2.0f;
        return new Vec3f(rmx[0][0]*v[0]+rmx[0][1]*v[1]+rmx[0][2]*v2+positionx,
                (rmx[1][0]*v[0]+rmx[1][1]*v[1]+rmx[1][2]*v2)+positiony,
                (rmx[2][0]*v[0]+rmx[2][1]*v[1]+rmx[2][2]*v2));
    }

    // square of the distance between two vectors
    float dist(Vec3f p0,Vec3f p1) {
        return (p0.x-p1.x)*(p0.x-p1.x)+(p0.y-p1.y)*(p0.y-p1.y)+(p0.z-p1.z)*(p0.z-p1.z);
    }

    // estimate of the square of the length of a bezier curve
    float bezier_length(Vec3f p0,Vec3f p1,Vec3f p2,Vec3f p3) {
        Vec3f m=p1.mid(p2);
        Vec3f t1=p0.mid(p1);
        Vec3f t5=p2.mid(p3);
        Vec3f t2=t1.mid(m);
        Vec3f t4=t5.mid(m);
        Vec3f t3=t2.mid(t4);
        return dist(t1,p0)+dist(t2,t1)+dist(t3,t2)+dist(t4,t3)+dist(t5,t3)+dist(p3,t5);
    }

    int clamp(int a,int min,int max) {
        int t = Math.max(a, min);
        return Math.min(t, max);
    }

    float clamp(float a, float max) {
        float t = Math.max(a, (float) 0);
        return Math.min(t, max);
    }

    // evaluate a bezier curve using the fast forward difference algorithm
    // see here:  https://www.scratchapixel.com/lessons/geometry/bezier-curve-rendering-utah-teapot/fast-forward-differencing.html
    void eval_bezier( int divs, float h, Vec3f p0, Vec3f p1, Vec3f p2, Vec3f p3, Vec3f[] out) {
        Vec3f b0 = new Vec3f(p0);
        Vec3f fph = p1.sub(p0).mul(3*h);
        Vec3f fpphh = p0.mul(6).sub(p1.mul(12)).add(p2.mul(6)).mul(h*h);
        Vec3f fppphhh = p0.mul(-6).add(p1.mul(18)).sub(p2.mul(18)).add(p3.mul(6)).mul(h*h*h);
        Vec3f fppphhh12 = fppphhh.mul(0.1666666f);
        Vec3f fppphhh2 = fppphhh.mul(0.5f);
        out[0] = new Vec3f(b0);
        for (int i = 1; i <= divs; ++i) {
            b0.x += fph.x + fpphh.x * .5f + fppphhh12.x;
            b0.y += fph.y + fpphh.y * .5f + fppphhh12.y;
            b0.z += fph.z + fpphh.z * .5f + fppphhh12.z;
            fph.x += fpphh.x + fppphhh2.x;
            fph.y += fpphh.y + fppphhh2.y;
            fph.z += fpphh.z + fppphhh2.z;
            fpphh.x += fppphhh.x;
            fpphh.y += fppphhh.y;
            fpphh.z += fppphhh.z;
            out[i]=new Vec3f(b0);
        }
    }

    void add_bezier_patch(Vec3f[][] p) {
        int PX=2;
        // a patch has 16 control points
        // we use the length of the 1d curves to decide how many divisions to use.
        float d1=bezier_length(p[0][0],p[0][1],p[0][2],p[0][3]);
        float d2=bezier_length(p[0][0],p[1][0],p[2][0],p[3][0]);
        float d3=bezier_length(p[0][3],p[1][3],p[2][3],p[3][3]);
        float d4=bezier_length(p[3][0],p[3][1],p[3][2],p[3][3]);

        float maxyd=(float)Math.sqrt(Math.max(d1,d4));
        float maxxd=(float)Math.sqrt(Math.max(d2,d3));
        // these are the x and y divisions
        int xdivs= (int) (maxxd/PX);
        int ydivs= (int) (maxyd/PX);

        // a min of 4 divs and a max of 20
         xdivs=clamp(xdivs,4,20);
         ydivs=clamp(ydivs,4,20);

        Vec3f[][] py=new Vec3f[4][ydivs+1];
        float h = 1.f / ydivs;
        for (int i=0; i<4; i++) {
            eval_bezier(ydivs, h, p[i][0], p[i][1], p[i][2], p[i][3], py[i]);
        }
        Vec3f[][] np=new Vec3f[2][xdivs+1];
        h = 1.f / xdivs;
        for (int i=0; i<=ydivs; i++) {
            eval_bezier(xdivs, h, py[0][i], py[1][i], py[2][i], py[3][i], np[i%2]);
            if(i>0) {
                int j1=i%2;
                int j0=1-j1;
                for (int k=0; k<xdivs; k++) {
                    add_quad(np[j0][k],np[j1][k],np[j1][k+1],np[j0][k+1]);
                }
            }
        }
    }
    void draw_all_quads(Graphics g) {
        //g.setPaintMode();
        for (int i = 0; i < ZSIZE; i++) {
            Quad q = quad_lists[i];
            while (q != null) {
                g.setColor(q.col);
                g.fillPolygon(q.px,q.py,4);
                q =q.next;
            }
        }
    }

    Teapot(int px, int py, float sz, Vec3f rot, Vec3f col) {
        material_colour=col;
        rotation=rot;
        positionx=px;
        positiony=py;
        size=sz;
    }

    void addrotation(Vec3f v) {
        rotation=rotation.add(v);
    }
    void setrotation(Vec3f v) {
        rotation=v;
    }

    Vec3f getrotation() {
        return rotation;
    }

    void draw(Graphics2D g) {
        quad_init();
        maketrotationmatrix(rotation,size);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_SPEED);
        for(int ii=0;ii<32;ii++) {
            // each patch is defined by 16 control points
            Vec3f[][] p = new Vec3f[4][4];
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    p[j][k] = vrotate(TeapotData.teapotVertices[TeapotData.teapotPatches[ii][j * 4 + k] - 1]);
                }
            }
            add_bezier_patch(p);
        }
        draw_all_quads(g);
    }
}
