import java.awt.*;
import java.awt.event.MouseEvent;

public class Main extends GameEngine {
    private Teapot teapot;
    private boolean paused;
    int px,py;
    Vec3f rot;
    // Main Function
    public static void main(String[] args) {
        createGame(new Main(), 60);
    }

    @Override
    public void init() {
        // make a chocolate teapot;
        teapot=new Teapot(256,256,50,new Vec3f(0,0,0),new Vec3f(100,50,50));
    }

    @Override
    public void update(double dt) {
        if(!paused)
            teapot.addrotation(new Vec3f(0.011f,0.019f,0.017f).mul((float)dt*100f));
    }

    @Override
    public void paintComponent() {
    //    changeBackgroundColor(Color.BLACK);
    //    clearBackground(512,512);
        changeColor(Color.BLACK);
        drawSolidRectangle(0,0,512,512);
        teapot.draw(mGraphics);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        teapot.setrotation(rot.add(new Vec3f((e.getX()-px)/100f,
                (e.getY()-py)/100f,0f)));
    }

    @Override
    public void mousePressed(MouseEvent event) {
        px=event.getX();
        py=event.getY();
        rot=teapot.getrotation();
        paused=true;
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        paused=false;
    }
}