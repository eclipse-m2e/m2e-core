package foo.bar;

import javax.persistence.Entity;

@Entity
public class TestDummy {
	
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
