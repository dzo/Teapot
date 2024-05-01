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
    private final int ZSIZE=1024;
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
    private float ambient_strength=0.25f;
    private Vec3f material_colour;
    private Vec3f light_colour=new Vec3f(255,255,255);
    private float specular_strength =0.65f;
    private float shininess=16;
    // teapot rotation
    private Vec3f rotation;
    private int flags=SPECULAR | DIFFUSE;
    public static final int WIREFRAME=1;
    public static final int COLOURPATCHES=2;
    public static final int SPECULAR=4;
    public static final int DIFFUSE=8;

    // centre of the window
    private Vec3f centre;
    private int quadSize =3;

    public int getQuadSize() {
        return quadSize;
    }

    public void setQuadSize(int quadSize) {
        if(quadSize<16 && quadSize>0)
           this.quadSize = quadSize;
    }
    public float getShininess() {
        return shininess;
    }

    public void setShininess(float shininess) {
        if(shininess>2 && shininess<200)
            this.shininess = shininess;
    }
    int getFlags() {
        return flags;
    }

    void setFlags(int f) {
        flags=f;
    }

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
    // simple perspective transform
    Vec3f perspective(Vec3f p) {
        Vec3f p1=p.sub(centre);
        float sc=p1.z/400f+1.0f;
        p1=p1.mul(sc).add(centre);
        return p1;
    }
    // add a quad to one of the lists and work out what colour to draw it.
    void add_quad(Vec3f p0, Vec3f p1, Vec3f p2, Vec3f p3) {
        // apply perspective transform to each point
        Vec3f p0p=perspective(p0);
        Vec3f p1p=perspective(p1);
        Vec3f p2p=perspective(p2);
        Vec3f p3p=perspective(p3);
        // get the normal vector
        Vec3f normal=p2p.sub(p0p).cross(p3p.sub(p0p));
        // don't draw it if it's facing away from us.
        if(normal.z<=0) return;
        normal.normalise();
        // get the diffuse lighting strength
        float dp=normal.dot(lightdir);
        float diff=clamp(dp, 1.0f);
        // diffuse lighting
        Vec3f diffuse=material_colour.mul(diff);
        // specular lighting
        float spec=clamp(2.0f*dp*normal.z-lightdir.z, 2);
        spec=(float)Math.pow(spec,shininess);
        spec= specular_strength *spec;
        Vec3f specular=light_colour.mul(spec);
        Vec3f res=material_colour.mul(ambient_strength);
        if((flags & DIFFUSE)!=0)
            res=res.add(diffuse);
        if((flags & SPECULAR)!=0)
            res=res.add(specular);
        Vec3f colour=new Vec3f(clamp(res.x, 255),clamp(res.y, 255),clamp(res.z, 255));
        // use average z value for the quad as the list index.
        // so they are drawn with the closest last
        int zindex=clamp((int)((p0p.z+p1p.z+p2p.z+p3p.z)/4)+ZSIZE/2,0,ZSIZE-1);
        Quad q= new Quad();
        
        q.px= new int[]{(int) (p0p.x), (int) (p1p.x), (int) (p2p.x), (int) (p3p.x)};
        q.py= new int[]{(int) (p0p.y), (int) (p1p.y), (int) (p2p.y), (int) (p3p.y)};
        q.col=new Color((int)colour.x,(int)colour.y,(int)colour.z);
        q.next=quad_lists[zindex];
        quad_lists[zindex]=q;
    }

    // apply the rotation matrix to a vector stored in the array of teapot vertices
    Vec3f vrotate(Vec3f v) {
        float v2=v.z-2.0f;
        return new Vec3f(rmx[0][0]*v.x+rmx[0][1]*v.y+rmx[0][2]*v2+positionx,
                (rmx[1][0]*v.x+rmx[1][1]*v.y+rmx[1][2]*v2)+positiony,
                (rmx[2][0]*v.x+rmx[2][1]*v.y+rmx[2][2]*v2));
    }

    // the distance between two vectors
    float dist(Vec3f p0,Vec3f p1) {
        return p0.sub(p1).length();
    }

    // estimate of the length of a bezier curve
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

        // a patch has 16 control points
        // we use the length of the 1d curves to decide how many divisions to use.
        float maxyd=0;
        float maxxd=0;
        for(int i=0;i<4;i++) {
            maxyd=Math.max(maxyd,bezier_length(p[i][0],p[i][1],p[i][2],p[i][3]));
            maxxd=Math.max(maxxd,bezier_length(p[0][i],p[1][i],p[2][i],p[3][i]));
        }
//        float d2=bezier_length(p[0][0],p[1][0],p[2][0],p[3][0]);
//        float d3=bezier_length(p[0][3],p[1][3],p[2][3],p[3][3]);
//        float d4=bezier_length(p[3][0],p[3][1],p[3][2],p[3][3]);

//        float maxyd=(float)(Math.max(d1,d4));
//        float maxxd=(float)(Math.max(d2,d3));
        // these are the x and y divisions
        int xdivs= (int) (maxxd/ quadSize);
        int ydivs= (int) (maxyd/ quadSize);

        // a min of 4 divs and a max of 40
        xdivs=clamp(xdivs,4,60);
        ydivs=clamp(ydivs,4,60);

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
        for (int i = 0; i < ZSIZE; i++) {
            Quad q = quad_lists[i];
            while (q != null) {
                g.setColor(q.col);
                if((flags & WIREFRAME)!=0)
                    g.drawPolygon(q.px,q.py,4);
                else
                    g.fillPolygon(q.px,q.py,4);

                q =q.next;
            }
        }
    }

    Teapot(Vec3f position, Vec3f rot, Vec3f col, Vec3f centre) {
        material_colour=col;
        rotation=rot;
        positionx=(int)(position.x+centre.x);
        positiony=(int)(position.y+centre.y);
        size=(int)(position.z+centre.z);
        this.centre=centre;
    }

    void setColour(Vec3f col) {
        material_colour=col;
    }

    void addRotation(Vec3f v) {
        rotation=rotation.add(v);
    }
    void setRotation(Vec3f v) {
        rotation=v;
    }

    Vec3f getRotation() {
        return rotation;
    }

    void draw(Graphics2D g) {
        quad_init();
        maketrotationmatrix(rotation,size);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_SPEED);
        for(int ii=0;ii<32;ii++) {
            if((flags & COLOURPATCHES)!=0)
                material_colour=new Vec3f((ii&0x3)*64.0f+63,(ii/16)*128.0f,((ii&0xc)/4)*64.0f+63);
            // each patch is defined by 16 control points
            Vec3f[][] p = new Vec3f[4][4];
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    float[] v=TeapotData.teapotVertices[TeapotData.teapotPatches[ii][j * 4 + k] - 1];
                    Vec3f vv=new Vec3f(v[0],v[1],v[2]);
                    if(ii>19&&ii<28) {
                        vv.x*=1.077;
                        vv.y*=1.077;
                    }
                    if(ii>15&&ii<18) {
                        if(j==0)
                            vv.x+=0.23;
                        if(j==1)
                            vv.z+=0.4;
                    }
                    p[j][k] = vrotate(vv);

                }
            }
            add_bezier_patch(p);
        }
        draw_all_quads(g);
    }
}
