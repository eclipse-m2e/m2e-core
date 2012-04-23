package foo.bar;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Dummy {
	private long id;
	@Id    
	public long getId() {
		return this.id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
