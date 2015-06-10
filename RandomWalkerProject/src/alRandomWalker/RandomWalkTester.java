package alRandomWalker;
import java.util.Random;

public class RandomWalkTester {

	public static void main(String[] args) {
		Random generator = new Random(); 
		DroneOne walker = new DroneOne(0, 0); 

		for (int i = 0; i < 100; i++) {
			// Get an index number and input the "size" of the enum to `nextInt`
			int directionIndex = generator.nextInt(Direction4.values().length);
			// Get the direction from the index generated above
			Direction4 dir = Direction4.values()[directionIndex];
			// Make the move:
			walker.move(dir);
			walker.report();
		} 
		walker.report();
	}
}
