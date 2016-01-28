package at.fhtw.mcs.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class ProjectTest {

	Project project = new Project();

	class NamedTrack extends NoOpTrack {
		private String name;

		public NamedTrack(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@Test
	public void testAddTrackAdds() throws Exception {
		Track t1 = new NamedTrack("guitar");
		Track t2 = new NamedTrack("guitar2");

		project.addTrack(t1);
		project.addTrack(t2);

		assertEquals(Arrays.asList(t1, t2), project.getTracks());
	}

	@Test
	public void testAddTrackHandlesNameCollision() throws Exception {
		Track t1 = new NamedTrack("guitar");
		Track t2 = new NamedTrack("guitar");

		project.addTrack(t1);
		project.addTrack(t2);

		assertEquals("guitar(2)", t2.getName());
	}

	@Test
	public void testAddTrackHandlesNameCollisionMultipleTimes() throws Exception {
		Track t1 = new NamedTrack("guitar");
		Track t2 = new NamedTrack("guitar");
		Track t3 = new NamedTrack("guitar");

		project.addTrack(t1);
		project.addTrack(t2);
		project.addTrack(t3);

		assertEquals("guitar(2)", t2.getName());
		assertEquals("guitar(3)", t3.getName());
	}
}
