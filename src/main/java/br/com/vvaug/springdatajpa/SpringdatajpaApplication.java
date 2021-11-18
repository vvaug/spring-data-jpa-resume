package br.com.vvaug.springdatajpa;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@SpringBootApplication
public class SpringdatajpaApplication {

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private EmployeePagingAndSortingRepository employeePagingAndSortingRepository;

	@Autowired
	private EmployeeSpecificationRepository employeeSpecificationRepository;

	public static void main(String[] args) {
		SpringApplication.run(SpringdatajpaApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(){
		return (this::run);
	}

	public void showAllRecords(){
		System.out.println("All Records: ");
		System.out.println(employeeRepository.findAll());
	}

	private void run(String... args) {

		employeeRepository.saveAll(Arrays.asList(new Employee("Victor", "Augusto", BigDecimal.valueOf(8500)),
				new Employee("Matheus", "Ramos", BigDecimal.valueOf(5200)),
				new Employee("Felipe", "Tomas", BigDecimal.valueOf(4320)),
				new Employee("Fulano", "XPTO", BigDecimal.valueOf(1250)),
				new Employee("Fulano", null, BigDecimal.valueOf(1250)))
		);

		showAllRecords();

		System.out.println("Updating a record: ");

		var employee = employeeRepository.findById(1L)
				.orElseThrow(() -> new RuntimeException());

		System.out.println(ToStringBuilder.reflectionToString(employee));

		employee.setFirstName("Vinicius");
		employee.setLastName("Melo");

		System.out.println("Saving new values: ");
		System.out.println(employee.toString());
		employeeRepository.save(employee);

		showAllRecords();

		var employees = employeeRepository.findByFirstName("Victor");
		System.out.println();
		employees.addAll(employeeRepository.findByLastName("Augusto"));
		System.out.println();
		employees.addAll(employeeRepository.findByFirstNameLike("Vi"));
		System.out.println();
		employees.addAll(employeeRepository.findByFirstNameAndLastName("Matheus","Ramos"));
		System.out.println();
		employees.addAll(employeeRepository.findByFirstNameEndingWith("s"));
		System.out.println();
		employees.addAll(employeeRepository.findByFirstNameStartingWith("f"));
		System.out.println();
		employees.addAll(employeeRepository.findByFirstNameIgnoreCaseStartingWith("f"));
		System.out.println();
		employees.addAll(employeeRepository.findByLastNameIsNotNull());
		System.out.println();
		employees.addAll(employeeRepository.findByLastNameIsNull());
		System.out.println();
		employees.addAll(employeeRepository.findByFirstNameOrderByFirstNameAsc("Victor"));
		System.out.println();
		employees.addAll(employeeRepository.findEmployeesWithCustomCriteriaUsingJpql("Victor", "Felipe",BigDecimal.valueOf(1000)));
		System.out.println();
		employees.addAll(employeeRepository.findEmployeesNativeQuery());
		System.out.println();
		//Pagination and Ordering
		Pageable criteria = PageRequest.of(1, 3, Sort.unsorted());
		var employeesPageable = employeePagingAndSortingRepository.findAll(criteria);

		var employeesProjection = employeeRepository.findEmployeesUsingProjections();

		employeesProjection.forEach(p -> System.out.println(p.getFirstName() + " | " + p.getLastName()));

		System.out.println("query using specification: ");

		employees.addAll(employeeSpecificationRepository.findAll(EmployeeSpecification.firstNameLike("Victor")));
		employees.addAll(employeeSpecificationRepository.findAll(EmployeeSpecification.lastNameLike("Ramos")));

		//Dynamic Query using Specifications

		employees.addAll(employeeSpecificationRepository.findAll(EmployeeSpecification.firstNameLike("Victor")
				.or(EmployeeSpecification.salaryGreatherThan(BigDecimal.valueOf(2000)))
				.or(EmployeeSpecification.lastNameLike("Ramos"))));
	}
}

@Repository
//JpaSpecificationExecutor -> responsible for executing specification queries
interface  EmployeeSpecificationRepository extends CrudRepository<Employee,Long>, JpaSpecificationExecutor<Employee> {

}
@Repository
interface EmployeePagingAndSortingRepository extends PagingAndSortingRepository<Employee, Long>{

}

@Repository
interface EmployeeRepository extends CrudRepository<Employee, Long> {

	//Derived queries

	List<Employee> findByFirstName(String firstName);
	List<Employee> findByLastName(String lastName);
	//and
	List<Employee> findByFirstNameAndLastName(String firstName, String lastName);
	//like
	List<Employee> findByFirstNameLike(String lastName /* arg must be between %arg% */);
	List<Employee> findByFirstNameStartingWith(String firstName);
	List<Employee> findByFirstNameEndingWith(String firstName);
	//null, not null
	List<Employee> findByLastNameIsNotNull();
	List<Employee> findByLastNameIsNull();
	//ordering
	List<Employee> findByFirstNameOrderByFirstNameAsc(String firstName);
	//ignoring case sensitive
	List<Employee> findByFirstNameIgnoreCaseStartingWith(String firstName);

	/*
		We should be careful with big method names because of cognitive complexity
	 	In case of using complex filters, consider using JPQL Queries
	 */

	//JPQL

	@Query("SELECT e FROM Employee e WHERE " +
			"( e.firstName = :firstName " +
			"	OR e.firstName = :auxFirstName ) " +
			"AND e.salary >= :salary")
	List<Employee> findEmployeesWithCustomCriteriaUsingJpql(String firstName, String auxFirstName, BigDecimal salary);

	//Native Query
	@Query(nativeQuery = true, value = "SELECT * FROM Employee")
	List<Employee> findEmployeesNativeQuery();

	//Interface-based Projection
	@Query(nativeQuery = true, value = "SELECT first_name firstName, last_name lastName FROM Employee")
	List<EmployeeProjection> findEmployeesUsingProjections();

}


//JPA Entity
@Entity
class Employee {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String firstName;
	private String lastName;
	private BigDecimal salary;

	public Employee(){

	}

	public Employee(String firstName, String lastName, BigDecimal salary){
		this.firstName = firstName;
		this.lastName = lastName;
		this.salary = salary;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public BigDecimal getSalary() {
		return salary;
	}

	public void setSalary(BigDecimal salary) {
		this.salary = salary;
	}

}

//Projections
interface EmployeeProjection {
	String getFirstName();
	String getLastName();
}

//Specifications
class EmployeeSpecification {

	public static final String FIRST_NAME = "firstName";
	public static final String LAST_NAME = "lastName";
	public static final String SALARY = "salary";

	public static final String LIKE_STMT = "%?%";

	public static Specification<Employee> firstNameLike(String firstName){
		return (root, criteriaQuery, criteriaBuilder) ->
			criteriaBuilder.like(root.get(FIRST_NAME), LIKE_STMT.replace("?", firstName));
	}

	public static Specification<Employee> lastNameLike(String lastName){
		return (root, criteriaQuery, criteriaBuilder) ->
				criteriaBuilder.like(root.get(LAST_NAME), LIKE_STMT.replace("?", lastName));
	}

	public static Specification<Employee> firstNameEquals(String firstName){
		return (root, criteriaQuery, criteriaBuilder) ->
				criteriaBuilder.equal(root.get(FIRST_NAME), firstName);
	}

	public static Specification<Employee> salaryGreatherThan(BigDecimal salary){
		return (root, criteriaQuery, criteriaBuilder) ->
				criteriaBuilder.greaterThan(root.get(SALARY), salary);
	}
}
