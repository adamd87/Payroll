package pl.adamd.payroll;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
public class EmployeeController {

    private final EmployeeRepository repository;

    private final EmployeeModelAssembler assembler;

    EmployeeController(EmployeeRepository repository, EmployeeModelAssembler assembler) {
        this.repository = repository;
        this.assembler = assembler;
    }

    //------------------------------------------------------------------------------------------------------------------
    @GetMapping("/employees")
    CollectionModel<EntityModel<Employee>> all() {
        List<EntityModel<Employee>> employees = repository.findAll().stream()
/*              .map(employee -> EntityModel.of(employee,
                      linkTo(methodOn(EmployeeController.class).one(employee.getId())).withSelfRel(),
                      linkTo(methodOn(EmployeeController.class).all()).withRel("employees")))

         ---- code has been replaced by:
 */
                .map(assembler::toModel) // injected into the controller ----
                .collect(Collectors.toList());

        return CollectionModel.of(employees,
                linkTo(methodOn(EmployeeController.class).all()).withSelfRel());
    }

    //------------------------------------------------------------------------------------------------------------------
    @GetMapping("/employees/{id}")
    EntityModel<Employee> one(@PathVariable Long id) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        return assembler.toModel(employee);
    }
/*
    ---- Code hase been moved into EmployeeModelAssembler.class and replaced by assembler.toModel(employee) ----
        return EntityModel.of(
                 employee,
                 linkTo(methodOn(EmployeeController.class).one(id)).withSelfRel(),
                 linkTo(methodOn(EmployeeController.class).all()).withRel("employees"));
*/

    //------------------------------------------------------------------------------------------------------------------
/*  --- previous version with the "name" field in the employee constructor ----
    @PostMapping("/employees")
    Employee newEmployee(@RequestBody Employee newEmployee) {
        return repository.save(newEmployee);
    }

    --- updated version ---
    In this case you can use the same endpoint to create a new employee resource, and use legacy "name" field
*/
    @PostMapping("/employees")
    ResponseEntity<?> newEmployee(@RequestBody Employee newEmployee) {
        EntityModel<Employee> entityModel = assembler.toModel(repository.save(newEmployee));

/*  ResponseEntity is used to create an HTTP 201 Created status message.
    Response typically includes a Location response header,
    and we use the URI derived from the model's self-related link
*/
        return ResponseEntity
                .created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(entityModel);
    }

    //------------------------------------------------------------------------------------------------------------------
    @PutMapping("/employees/{id}")
    ResponseEntity<EntityModel<Employee>> replaceEmployee(@RequestBody Employee newEmployee, @PathVariable Long id) {
        Employee updatedEmployee = repository.findById(id)
                .map(employee -> {
                    employee.setName(newEmployee.getName());
                    employee.setRole(newEmployee.getRole());
                    return repository.save(employee);
                })
                .orElseGet(() -> {
                    newEmployee.setId(id);
                    return repository.save(newEmployee);
                });

/*      The Employee object built from the save() operation in then wrapped using the EmployeeModelAssembler
        into an EntityModel<Employee> object
*/
        EntityModel<Employee> entityModel = assembler.toModel(updatedEmployee);

        return ResponseEntity
                .created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(entityModel);
    }

    //------------------------------------------------------------------------------------------------------------------
/*  --- previous version ---
    @DeleteMapping("/employees/{id}")
    void deleteEmployee(@PathVariable Long id) {
        repository.deleteById(id);
    }
    --- updated version: ---
*/
    @DeleteMapping("/employees/{id}")
    ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        repository.deleteById(id);

        // returns an HTTP 204 No Content response:
        return ResponseEntity.noContent().build();
    }
}
