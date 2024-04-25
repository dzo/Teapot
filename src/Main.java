import java.awt.*;
import java.awt.event.*;

public class Main extends GameEngine {
    private Teapot teapot;
    private Vec3f colour=new Vec3f(100,50,50);
    private boolean paused;
    private int w=1024,h=768;
    int px,py;
    Vec3f rot;
    float fps=60;
    // Main Function
    public static void main(String[] args) {
        createGame(new Main(), 60);
    }

    @Override
    public void init() {
        setWindowSize(w, h);
        // make a chocolate teapot;
        teapot=new Teapot(new Vec3f(0,0,80),new Vec3f(0,0,0),colour,new Vec3f(w/2,h/2,0));
        teapot.setFlags(Teapot.DIFFUSE | Teapot.SPECULAR | Teapot.COLOURPATCHES);
    }

    @Override
    public void update(double dt) {
        if(!paused)
            teapot.addRotation(new Vec3f(0.011f,0.019f,0.017f).mul((float)dt*100f));
        if(dt!=0)
            fps=fps*0.9f+(float)(1.0f/dt)*0.1f;
    }

    @Override
    public void paintComponent() {
        changeColor(Color.BLACK);
        drawSolidRectangle(0,0,width(),height());
        teapot.setColour(colour);
        teapot.draw(mGraphics);
        changeColor(Color.WHITE);
        drawText(10,32,"Fps:"+(int)fps+" Quad Size:"+teapot.getQuadSize()+" Shininess:"+teapot.getShininess(),"Arial",16);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        teapot.setRotation(rot.add(new Vec3f((e.getX()-px)/100f,
                (e.getY()-py)/100f,0f)));
    }

    @Override
    public void mousePressed(MouseEvent event) {
        px=event.getX();
        py=event.getY();
        rot=teapot.getRotation();
        paused=true;
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        paused=false;
    }

    @Override
    public void keyPressed(KeyEvent event) {
        int k=event.getKeyCode();
        int flags= teapot.getFlags();
        switch(k) {
            case KeyEvent.VK_W: flags=flags ^ Teapot.WIREFRAME;
            break;
            case KeyEvent.VK_S: flags=flags ^ Teapot.SPECULAR;
            break;
            case KeyEvent.VK_C: flags=flags ^ Teapot.COLOURPATCHES;
            break;
            case KeyEvent.VK_D: flags=flags ^ Teapot.DIFFUSE;
            break;
            case KeyEvent.VK_UP: teapot.setQuadSize(teapot.getQuadSize()+1);
            break;
            case KeyEvent.VK_DOWN: teapot.setQuadSize(teapot.getQuadSize()-1);
            break;
            case KeyEvent.VK_RIGHT: teapot.setShininess(teapot.getShininess()*2);
            break;
            case KeyEvent.VK_LEFT: teapot.setShininess(teapot.getShininess()/2);
            break;

        }
        teapot.setFlags(flags);
    }
}