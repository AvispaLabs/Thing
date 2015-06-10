package alRandomWalker;
public class DroneOne {
    int x, y; 

    DroneOne(int x, int y) {
		this.x = x; 
		this.y = y;
    } 
	
	void move(Direction4 direction) {
		this.x += direction.getX();
		this.y += direction.getY();
	}
    void report() {
		System.out.println("Location: " + x + ", " + y); 
    } 
}
