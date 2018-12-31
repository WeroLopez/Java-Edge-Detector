package EdgeDetector;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;


public class EdgeDetector extends JFrame{
    
    EDPanel pt;
    
    public static void main(String[] args) {
        EdgeDetector t = new EdgeDetector();
    }
    
    public EdgeDetector(){
        setTitle("Canny edge detector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        BufferedImage image = null;
        try {
            image = ImageIO.read(this.getClass().getResource("/images/imagen1.png"));
            int w = image.getWidth();
            int h = image.getHeight();
            System.out.println("Size: "+w);
            if(w!=h){
                JOptionPane.showMessageDialog(null, "La imagen debe ser cuadrada", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        
        pt = new EDPanel(image);
        getContentPane().add(pt);
        setVisible(true);
        pack();
        setLocationRelativeTo(null);
    }
    
}