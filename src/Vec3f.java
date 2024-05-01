// helper class for 3d vectors
public class Vec3f {
    public float x,y,z;
    Vec3f(float x,float y, float z) {
        this.x=x;
        this.y=y;
        this.z=z;
    }
    Vec3f(Vec3f p) {
        x=p.x;
        y=p.y;
        z=p.z;
    }
    Vec3f sub(Vec3f p) {
        return new Vec3f(x - p.x, y - p.y, z - p.z);
    }
    Vec3f add(Vec3f p) {
        return new Vec3f(x+p.x, y+p.y, z+p.z);
    }
    Vec3f cross(Vec3f p) {
        return new Vec3f(y*p.z-z*p.y,
                z*p.x-x*p.z,
                x*p.y-y*p.x);
    }
    Vec3f mul(float f) {
        return new Vec3f(f*x, f*y, f*z);
    }
    Vec3f mid(Vec3f p) {
        return new Vec3f((p.x+x)*.5f, (p.y+y)*.5f, (p.z+z)*.5f);
    }
    float length() {
        return (float)Math.sqrt(x*x+y*y+z*z);
    }
    void normalise() {
        float mag=1/length();
        x=x*mag;
        y=y*mag;
        z=z*mag;
    }
    float dot(Vec3f p) {
        return ((x * p.x) + (y * p.y) + (z * p.z));
    }
}
