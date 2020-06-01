package com.rest.spring.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.rest.spring.assembler.EmployeeModelAssembler;
import com.rest.spring.entities.Employee;
import com.rest.spring.exception.EmployeeNotFoundException;
import com.rest.spring.repository.EmployeeRepository;

@RestController
public class EmployeeController {
	@Autowired EmployeeRepository repository;
	@Autowired EmployeeModelAssembler employeeModelAssembler;

	public EmployeeController(EmployeeRepository repository, EmployeeModelAssembler employeeModelAssembler) { 
		this.repository =repository; 
		this.employeeModelAssembler = employeeModelAssembler;
	}


	// Aggregate root

	@GetMapping("/employees")
	public CollectionModel<EntityModel<Employee>>  all() {
		List<EntityModel<Employee>> employees = repository.findAll().stream()
				.map(employeeModelAssembler::toModel)
				.collect(Collectors.toList());

		return CollectionModel.of(employees, linkTo(methodOn(EmployeeController.class).all()).withSelfRel());
	}

	@PostMapping("/employees")
	public ResponseEntity<?> newEmployee(@RequestBody Employee newEmployee) {
		EntityModel<Employee> employeeModel = employeeModelAssembler.toModel(repository.save(newEmployee));
		return ResponseEntity 
				.created(employeeModel.getRequiredLink(IanaLinkRelations.SELF).toUri()) 
				.body(employeeModel);
	}

	// Single item

	@GetMapping("/employees/{id}")
	public EntityModel<Employee> one(@PathVariable Long id) {

		Employee employee = repository.findById(id) //
				.orElseThrow(() -> new EmployeeNotFoundException(id));

		return employeeModelAssembler.toModel(employee);
	}

	@PutMapping("/employees/{id}")
	public ResponseEntity<?>  replaceEmployee(@RequestBody Employee newEmployee, @PathVariable Long id) {

		Employee updatedEmployee = repository.findById(id) //
				.map(employee -> {
					employee.setName(newEmployee.getName());
					employee.setRole(newEmployee.getRole());
					return repository.save(employee);
				}) //
				.orElseGet(() -> {
					newEmployee.setId(id);
					return repository.save(newEmployee);
				});

		EntityModel<Employee> entityModel = employeeModelAssembler.toModel(updatedEmployee);

		return ResponseEntity //
				.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()) //
				.body(entityModel);
	}

	@DeleteMapping("/employees/{id}")
	public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
		repository.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
