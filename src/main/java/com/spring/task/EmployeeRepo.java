package com.spring.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EmployeeRepo extends JpaRepository<Employee,Integer>
{
    public List<Employee> findAllByOrderByDesignation_levelAscEmpNameAsc();
    public Employee findByEmpId(int id);
    public List<Employee> findAllByParentIdAndEmpNameIsNot(int parentId,String empName);
    public List<Employee> findAllByParentId(Integer eid);
}

