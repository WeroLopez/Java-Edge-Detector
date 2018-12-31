package EdgeDetector;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.text.DecimalFormat;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

class EDPanel extends JPanel{
    
    final int size; //width/height of the image
    BufferedImage imageOriginal,imageKerneled,imageKerneled2; 
    BufferedImageOp imageop;
    
    final int squareSize = 16; //square size in pixels
    int imageGridSize; //total of squares that fit in your image
    
     int kernelSize = 5; //total size for the filter kernel
     double kernel[][]; //the filter kernel
     int kernelCenter; //center number of the filter kernel
     int kernelSum;
     
     int colorKernel[][];
     int colorKernel2[][];
     
    public EDPanel(BufferedImage image){
        size = image.getWidth();
        imageGridSize = size/squareSize;
        
        imageop = new AffineTransformOp(new AffineTransform(),AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        this.imageOriginal = imageop.filter(image, null);
        this.imageKerneled = imageop.filter(image, null);
        this.imageKerneled2 = imageop.filter(image, null);
        
        setPreferredSize(new Dimension(size*2,size));
        
        if ( (kernelSize & 1) == 0 ){ //matrix size must be odd number 
            JOptionPane.showMessageDialog(null, "Matrix size must be odd number", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        } 
        //setUglyKernel();
        //setGaussianKernel();
        
        setGaussianKernel2();
        doBlur(2);
        doBright(1.1);
        doGrayScale();
        findEdges();
        
        //initColorKernel();
    }
    
    @Override
    public void paintComponent(Graphics graphics){
        super.paintComponent(graphics); 
        Graphics2D g2d = (Graphics2D)graphics;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(imageOriginal, imageop, size, 0);
        g2d.drawImage(imageKerneled, imageop, 0, 0);
        
        //paintSnowflake(g2d);
        //paintSqaures(g2d);
        //paintColorKernel(g2d);
    }
    
    
    //This method does a sobel edge detector followed by a canny edge detector
    private void findEdges(){
        int pixel,x,y;
        int color;
        double gx,gy,magnitude;
        double magnitudes[][] = new double[size-2][size-2];
        double angles[][] = new double[size-2][size-2];
        int directions[][] = new int[size-2][size-2];
        //give numbers to the edge kernel
        int edgeKernelX[][] = new int[][]{{-1, 0, 1},
                                          {-2, 0, 2},
                                          {-1, 0, 1}};
        int edgeKernelY[][] = new int[][]{{-1, -2, -1},
                                          { 0,  0,  0},
                                          { 1,  2,  1}};
        //march the image
        for (int i = 1; i < size-1; i++) {
            for (int j = 1; j < size-1; j++) {
                //reset color
                gx = gy = 0;
                //march pixels inside kernel
                for (int k = 0; k < 3; k++) {
                    for (int l = 0; l < 3; l++) {
                        //get gray color from pixel
                        x = j + l - 1; 
                        y = i + k - 1;
                        pixel = imageKerneled.getRGB(x,y);
                        //multiply the rgb by the number in kernel
                        color = ((pixel) & 0xff);
                        gx += color * edgeKernelX[k][l];
                        gy += color * edgeKernelY[k][l];
                    }
                }
                //get magnitude for the edge ( Gxy = sqrt(x^2+y^2) )
                magnitude = (int)Math.round(Math.sqrt((gx*gx)+(gy*gy) ) );
                magnitudes[i-1][j-1] = magnitude;
                //get angle for the edge ( A = arctan Gy/Gx )
                angles[i-1][j-1] = Math.atan(gy/gx);
            }
        }
        //get bigger magnitud for rule of 3 because are magnitudes above 255
        double bigger = magnitudes[0][0]; int sizeMinus2 = size-2;
        for (int i = 0; i < sizeMinus2; i++) {
            for (int j = 0; j < sizeMinus2; j++) {
                if(magnitudes[i][j] > bigger) bigger = magnitudes[i][j];
            }
        }
        //rule of 3 to make all magnitudes below 255
        for (int i = 0; i < sizeMinus2; i++) {
            for (int j = 0; j < sizeMinus2; j++) {
                magnitudes[i][j] = magnitudes[i][j] * 255 / bigger;
            }
        }
        //set the borders black because there are no magnitudes there
        for (int i = 0; i < size; i++) {imageKerneled2.setRGB(0,i,Color.BLACK.getRGB());}
        for (int i = 0; i < size; i++) {imageKerneled2.setRGB(size-1,i,Color.BLACK.getRGB());}
        for (int i = 0; i < size; i++) {imageKerneled2.setRGB(i,0,Color.BLACK.getRGB());}
        for (int i = 0; i < size; i++) {imageKerneled2.setRGB(i,size-1,Color.BLACK.getRGB());}
        //set the magnitudes in the image
        double degrees;
        double max=0,min=0;
        for (int i = 0; i < sizeMinus2; i++) {
            for (int j = 0; j < sizeMinus2; j++) {
                //paint black the NaN and 0r angles
                magnitude = magnitudes[i][j];
                if(Double.isNaN(angles[i][j]) || angles[i][j]==0){
                    imageKerneled2.setRGB(j+1,i+1,Color.BLACK.getRGB());
                }
                else{
                    //convert radians to dregrees for HSB (Hue goes from 0 to 360)
                    degrees = Math.toDegrees(angles[i][j]); 
                    //convert degrees in scale from -90,90 to 0,360 for HSBtoRGB with a rule of 3
                    degrees = degrees * 360 / 90; 
                    //convert degrees in scale from 0,360 to 0,1 for HSBtoRGB with a rule of 3
                    degrees /= 360;
                    /*if want angles with colors: 
                    //convert magnitud in scale from 0,255 to 0,1 for HSBtoRGB with a rule of 3
                    magnitude /= 255;
                    Color c = new Color(Color.HSBtoRGB((float)degrees,1.0f,(float)magnitude));*/
                    Color c = new Color((int)magnitude,(int)magnitude,(int)magnitude);
                    imageKerneled2.setRGB(j+1,i+1,c.getRGB());
                    /*set direction for pixel
                    east-west:           0 = 0  - 22, 158-202, 338-360
                    northeast-southwest: 1 = 23 - 67, 203-247 
                    north-south:         2 = 68 -112, 248-292 
                    northeast-southeast: 3 = 113-157, 293-337 */
                    if((degrees>=0 && degrees<=22) || (degrees>=158 && degrees<=202) || (degrees>=338 && degrees<=360)){
                        directions[i][j] = 0;
                    }else if((degrees>=23 && degrees<=67) || (degrees>=203 && degrees<=247)){
                        directions[i][j] = 1;
                    }else if((degrees>=68 && degrees<=112) || (degrees>=248 && degrees<=292)){
                        directions[i][j] = 2;
                    }else if((degrees>=113 && degrees<=157) || (degrees>=293 && degrees<=337)){
                        directions[i][j] = 3;
                    }
                }
            }
        }
        copyKerneled2ToKerneled();
        System.out.println("Finished sobel edge detector");
        //here starts the canny edge detector
        int comparedMagnitudes[] = new int [3];
        for (int i = 1; i < size-1; i++) {
            for (int j = 1; j < size-1; j++) {
                //get magnitude of current pixel and neighbors from direction
                comparedMagnitudes[0] = (imageKerneled.getRGB(j,i)) & 0xff;
                switch(directions[i-1][j-1]){
                    case 0: 
                        comparedMagnitudes[1] = (imageKerneled.getRGB(j-1,i)) & 0xff;
                        comparedMagnitudes[2] = (imageKerneled.getRGB(j+1,i)) & 0xff;
                        break;
                    case 1:
                        comparedMagnitudes[1] = (imageKerneled.getRGB(j+1,i+1)) & 0xff;
                        comparedMagnitudes[2] = (imageKerneled.getRGB(j-1,i-1)) & 0xff;
                        break;
                    case 2:
                        comparedMagnitudes[1] = (imageKerneled.getRGB(j,i-1)) & 0xff;
                        comparedMagnitudes[2] = (imageKerneled.getRGB(j,i+1)) & 0xff;
                        break;
                    case 3:
                        comparedMagnitudes[1] = (imageKerneled.getRGB(j-1,i+1)) & 0xff;
                        comparedMagnitudes[2] = (imageKerneled.getRGB(j+1,i-1)) & 0xff;
                        break;
                }
                if(comparedMagnitudes[0]<comparedMagnitudes[1] 
                   || comparedMagnitudes[0]<comparedMagnitudes[2]
                   || comparedMagnitudes[0]<30){
                    imageKerneled2.setRGB(j,i,Color.BLACK.getRGB());
                }
            }
        }
        /*for (int i = 1; i < size-1; i++) {
            for (int j = 1; j < size-1; j++) {
                color = (imageKerneled2.getRGB(j,i)) & 0xff;
                if(color > 0){
                    imageKerneled2.setRGB(j,i,Color.WHITE.getRGB());
                }
            }
        }*/
        copyKerneled2ToKerneled();
        System.out.println("Finished canny edge detector");
    }
    private void doBright(double bright){
        int pixel, a,r,g,b,avg;
        Color gray;
        //march squares
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                //get rgb color from pixel with the birhgt modify
                pixel = imageKerneled.getRGB(j,i);
                r = (int)(((pixel >> 16) & 0xff) * bright);
                g = (int)(((pixel >> 8) & 0xff) * bright);
                b = (int)(((pixel) & 0xff) * bright);
                //regulate the numbers
                if(r>255) {r=255;}
                else if(r<0) {r=0;}
                if(g>255) {g=255;}
                else if(g<0) {g=0;}
                if(b>255) {b=255;}
                else if(b<0) {b=0;}
                //new brighter color for pixel
                gray = new Color(r,g,b);
                imageKerneled2.setRGB(j,i,gray.getRGB());
            }
        }
        copyKerneled2ToKerneled();
        System.out.println("Finished bright: x"+bright);
    }
    private void doBlur(int times){
        //Graphics2D g2d = (Graphics2D) graphics;
        //g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        
        int pixel, x,y;
        long s,r,g,b;
        //times the image will be blurred with the kernel
        for (int t = 0; t < times; t++) {
            //march pixels
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    //reset colors
                    r = g = b = s = 0;
                    //march pixels inside kernel
                    for (int k = 0; k < kernelSize; k++) {
                        for (int l = 0; l < kernelSize; l++) {
                            //get rgb color from pixel
                            x = j + l - kernelCenter; 
                            y = i + k - kernelCenter;
                            try{
                                if( x>=0 && x<512 && y>=0 && y<512 ){
                                    pixel = imageKerneled.getRGB(x,y);
                                    //multiply the rgb by the number in kernel
                                    r += ((pixel >> 16) & 0xff) * kernel[k][l];
                                    g += ((pixel >> 8) & 0xff) * kernel[k][l];
                                    b += ((pixel) & 0xff) * kernel[k][l];
                                    s += kernel[k][l];
                                }
                            }catch(ArrayIndexOutOfBoundsException e){
                                System.out.println("Error en "+x+","+y);
                            }
                        }
                    }
                    //averages
                    r = Math.round(r/s);
                    if(r>255) {System.out.println(r+" entro r > 255 en "+j+","+i); r=255; }
                    else if(r<0) {System.out.println(r+" entro r < 255 en "+j+","+i); r=0; }
                    g = Math.round(g/s);
                    if(g>255) {System.out.println(g+" entro g > 255 en "+j+","+i); g=255; }
                    else if(g<0) {System.out.println(g+" entro g < 255 en "+j+","+i); g=0; }
                    b = Math.round(b/s);
                    if(b>255) {System.out.println(b+" entro b > 255 en "+j+","+i); b=255; }
                    else if(b<0) {System.out.println(b+" entro b < 255 en "+j+","+i); b=0; }
                    //set the new rgb
                    imageKerneled2.setRGB(j,i,new Color((int)r,(int)g,(int)b).getRGB());
                }
            }
            copyKerneled2ToKerneled();
            System.out.println("Finished blur: "+(t+1));
        }
    }
    private void doGrayScale(){
        int pixel, a,r,g,b,avg;
        Color gray;
        //march squares
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                //get rgb color from pixel
                pixel = imageKerneled.getRGB(j,i);
                r = (pixel >> 16) & 0xff;
                g = (pixel >> 8) & 0xff;
                b = (pixel) & 0xff;
                //average from the 3 colors
                avg = (r+g+b)/3;
                //new gray color for pixel
                gray = new Color(avg,avg,avg);
                imageKerneled2.setRGB(j,i,gray.getRGB());
            }
        }
        copyKerneled2ToKerneled();
        System.out.println("Finished grayscale");
    }
    
    private void copyKerneled2ToKerneled(){
        ColorModel cm = imageKerneled2.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = imageKerneled2.copyData(imageKerneled2.getRaster().createCompatibleWritableRaster());
        imageKerneled = new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
    
    private void setGaussianKernel2(){
        kernelSum = 0; //sum of matrix
        kernelSize = 5;
        System.out.println("Gaussian kernel size: "+kernelSize);
        kernelCenter = (int)Math.floor(kernelSize/2.0); //find de center of matrix
        System.out.println("Gaussian kernel center: "+kernelCenter);
        //give numbers to the matrix
        kernel = new double[][]{{1, 4,  7,  4,  1},
                                {4, 16, 26, 16, 4},
                                {7, 26, 41, 26, 7},
                                {4, 16, 26, 16, 4},
                                {1, 4,  7,  4,  1}};
        for (int i = 0; i < kernelSize; i++) {
            for (int j = 0; j < kernelSize; j++) {
                kernelSum+=kernel[i][j];
            }
        }
        System.out.println("Gaussian kernel sum: "+kernelSum);
    }
    private void setGaussianKernel(){
        DecimalFormat df = new DecimalFormat( "0.0" );
        kernelSum = 0; //sum of matrix
        System.out.println("Gaussian kernel size: "+kernelSize);
        kernelCenter = (int)Math.floor(kernelSize/2.0); //find de center of matrix
        System.out.println("Gaussian kernel center: "+kernelCenter);
        kernel = new double[kernelSize][kernelSize]; //set the matrix
        //give numbers to the matrix
        double o=1,s=0,x,y;
        for (int i = 0; i < kernelSize; i++) {
            y = i - kernelCenter;
            for (int j = 0; j < kernelSize; j++) {
                x = j - kernelCenter;
                kernel[i][j] = (1/(2*Math.PI*o*o)) *  Math.exp( -( ((x*x)+(y*y)) / (2*o*o)  )  );
                s += kernel[i][j];
            }
        }
        //normalizar 
        for (int i = 0; i < kernelSize; i++) {
            for (int j = 0; j < kernelSize; j++) {
                kernel[i][j] /= s;
            }
        }
        double d = kernel[0][0]; 
        for (int i = 0; i < kernelSize; i++) {
            for (int j = 0; j < kernelSize; j++) {
                kernel[i][j] = Math.round(kernel[i][j]/d);
                System.out.print(df.format(kernel[i][j])+"\t");
                kernelSum+=kernel[i][j];
            }
            System.out.println("");
        }
        System.out.println("Gaussian kernel sum: "+kernelSum);
    }
    private void setUglyKernel(){
        kernelSum = 0; //sum of matrix
        kernelCenter = (int)Math.floor(kernelSize/2.0); //find de center of matrix
        //System.out.println("Center: "+matrixCenter);
        kernel = new double[kernelSize][kernelSize]; //set the matrix
        //give numbers to the matrix
        for (int i = 0; i < kernelSize; i++) {
            for (int j = 0; j < kernelSize; j++) {
                if(i==0 && j==0){kernel[i][j] = 1;}
                else{
                    if(j==0){
                        kernel[i][j] = (i>kernelCenter)? kernel[i-1][j]/2 : kernel[i-1][j]*2;
                    }
                    else {
                        kernel[i][j] = (j>kernelCenter)? kernel[i][j-1]/2 : kernel[i][j-1]*2;
                    }
                }
                kernelSum += kernel[i][j];
                System.out.print(kernel[i][j]+"\t");
            }
            System.out.println("");
        }
        System.out.println("Sum for average: "+kernelSum);
    }
    
    private void paintColorKernel(Graphics2D graphics){
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Shape shape;
        
        Color c = new Color(0,0,0);
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                switch(colorKernel[i][j]){
                    case 0: c = Color.BLACK; break;
                    case 1: c = Color.BLUE; break;
                    case 2: c = Color.YELLOW; break;
                    case 3: c = Color.RED; break;
                }
                g2d.setColor(c);
                shape = new Rectangle2D.Double(j*50,i*50,50,50);
                g2d.fill(shape);
                
                switch(colorKernel2[i][j]){
                    case 0: c = Color.BLACK; break;
                    case 1: c = Color.BLUE; break;
                    case 2: c = Color.YELLOW; break;
                    case 3: c = Color.RED; break;
                }
                g2d.setColor(c);
                shape = new Rectangle2D.Double(j*50+510,i*50,50,50);
                g2d.fill(shape);
            }
        }
    }
    private void initColorKernel(){
        colorKernel = new int[][] { {0,0,0,0,0,0,0,0,0,0},
                                    {0,2,1,1,0,0,2,0,0,0},
                                    {0,1,2,2,2,2,0,0,0,0},
                                    {0,1,0,0,0,1,0,0,0,0},
                                    {0,0,1,0,3,3,1,0,0,0},
                                    {0,0,0,3,1,1,3,0,0,0},
                                    {0,0,0,0,3,0,0,3,0,0},
                                    {0,0,0,0,0,3,2,3,0,0},
                                    {0,0,0,0,2,2,3,2,2,0},
                                    {0,0,0,2,0,0,0,0,0,2}};
        colorKernel2 = new int[][]{ {0,0,0,0,0,0,0,0,0,0},
                                    {0,0,0,0,0,0,0,0,0,0},
                                    {0,0,0,0,0,0,0,0,0,0},
                                    {0,0,0,0,0,0,0,0,0,0},
                                    {0,0,0,0,3,3,0,0,0,0},
                                    {0,0,0,3,0,0,3,0,0,0},
                                    {0,0,0,0,3,0,0,3,0,0},
                                    {0,0,0,0,0,3,2,3,0,0},
                                    {0,0,0,0,2,2,3,2,2,0},
                                    {0,0,0,2,0,0,0,0,0,2}};
    }
    
    public void paintSqaures(Graphics2D graphics){
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Shape shape;
        
        int pixel, a,r,g,b;
        int ss2 = squareSize*squareSize;
        //march squares
        for (int i = 0; i < imageGridSize; i++) {
            for (int j = 0; j < imageGridSize; j++) {
                //reset colors
                a = r = g = b = 0;
                //march pixels inside square
                for (int k = 0; k < squareSize; k++) {
                    for (int l = 0; l < squareSize; l++) {
                        //get rgb color from pixel
                        pixel = imageOriginal.getRGB((j*squareSize)+l, (i*squareSize)+k);
                        a += (pixel >> 24) & 0xff;
                        r += (pixel >> 16) & 0xff;
                        g += (pixel >> 8) & 0xff;
                        b += (pixel) & 0xff;
                    }
                }
                //averages
                a = Math.round(a/ss2);
                r = Math.round(r/ss2);
                g = Math.round(g/ss2);
                b = Math.round(b/ss2);
                //paint single square
                g2d.setColor(new Color(r,g,b,a));
                shape = new Rectangle2D.Double(j*squareSize,i*squareSize,squareSize,squareSize);
                g2d.fill(shape);
            }
        }
    }
   
    public void paintSnowflake(Graphics2D graphics){
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        GeneralPath gp1,gp2;
        Shape shape;
        AffineTransform af;
        float[] dist;
        Color[] colors;
        
        //fondo
        dist = new float[]{0.0f,1f};
        colors = new Color[]{
            new Color(188, 225, 242),
            new Color(166, 206, 221)
        };
        g2d.setPaint(new RadialGradientPaint(350, 350, 700, dist, colors));
        shape = new Rectangle2D.Double(0,0,size,size);
        g2d.fill(shape);
        
        //picote
        dist = new float[]{0.0f,.6f};
        colors = new Color[]{
            new Color(169, 215, 232),
            new Color(255, 255, 255)
        };
        g2d.setPaint(new RadialGradientPaint(350, 350, 500, dist, colors));
        gp1 = new GeneralPath();
        gp1.moveTo(350,350);
        gp1.lineTo(270,120);
        gp1.lineTo(350,50);
        gp1.lineTo(430,120);
        gp1.closePath();
        g2d.fill(gp1);
        g2d.setPaint(new RadialGradientPaint(350, 350, 450, dist, colors));
        for (int i = 0; i < 5; i++) {
            af = new AffineTransform();
            af.setToRotation((Math.PI/3)*(i+1), 350,350);
            shape = af.createTransformedShape(gp1);
            g2d.fill(shape);
        }
        
        //piquito
        g2d.setPaint(new RadialGradientPaint(350, 350, 300, dist, colors));
        gp2 = new GeneralPath();
        gp2.moveTo(350,350);
        gp2.lineTo(330,210);
        gp2.lineTo(350,160);
        gp2.lineTo(370,210);
        gp2.closePath();
        af = new AffineTransform();
        af.setToRotation((Math.PI/6), 350,350);
        shape = af.createTransformedShape(gp2);
        g2d.fill(shape);
        for (int i = 0; i < 11; i++) {
            if( (i&1)==0){
                af = new AffineTransform();
                af.setToRotation((Math.PI/6)*(i+1), 350,350);
                shape = af.createTransformedShape(gp2);
                g2d.fill(shape);
            }
        }
        
        //picote chico
        dist = new float[]{0.0f,.6f};
        colors = new Color[]{
            new Color(194, 227, 239),
            new Color(255, 255, 255)
        };
        g2d.setPaint(new RadialGradientPaint(350, 350, 200, dist, colors));
        af = new AffineTransform();
        af.setToScale(.4,.6);
        shape = af.createTransformedShape(gp1);
        af.setToTranslation(210, 140);    
        shape=af.createTransformedShape(shape);
        g2d.fill(shape);
        Shape shape1;
        for (int i = 0; i < 5; i++) {
            af = new AffineTransform();
            af.setToRotation((Math.PI/3)*(i+1), 350,350);
            shape1 = af.createTransformedShape(shape);
            g2d.fill(shape1);
        }
        
        //piquito chico
        g2d.setPaint(new RadialGradientPaint(350, 350, 100, dist, colors));
        af = new AffineTransform();
        af.setToScale(.5,.8);
        shape = af.createTransformedShape(gp2);
        af.setToTranslation(210, 140);    
        shape=af.createTransformedShape(shape);
        for (int i = 0; i < 11; i++) {
            if( (i&1)==0){
                af = new AffineTransform();
                af.setToRotation((Math.PI/6)*(i+1), 350,350);
                shape1 = af.createTransformedShape(shape);
                g2d.fill(shape1);
            }
        }
    }
    
    
}