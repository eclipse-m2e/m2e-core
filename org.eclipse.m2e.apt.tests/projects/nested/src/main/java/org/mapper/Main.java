package org.mapper;

public class Main {
	public static void main(String[] args) {
		Person person = new Person(1l, "test");
		PersonDto personDto = PersonMapper.INSTANCE.personToPersonDto(person);
		System.out.println(personDto);
	}
}
