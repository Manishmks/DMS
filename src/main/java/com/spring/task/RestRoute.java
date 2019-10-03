package com.spring.task;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Array;
import java.util.*;

@RestController
public class RestRoute
{
    @Autowired
    EmployeeRepo empRepo;
    @Autowired
    DesignationRepo degRepo;

    @GetMapping(path = "/home",produces = "application/json")
    public ResponseEntity<List<Employee>> allEmployee()
    {
        //return empRepo.findAllByOrderByDesignation_levelAscEmpNameAsc();
        List<Employee> list=empRepo.findAllByOrderByDesignation_levelAscEmpNameAsc();
        return new ResponseEntity<>(list,HttpStatus.OK);
    }

    @PostMapping(path = "/home")
    public ResponseEntity<String> saveData(@RequestBody EmployeePost employee)
    {
        HttpStatus status=null;
        String res="";

        String empName=employee.getEmpName();
        String desg=employee.getEmpDesg();
        int parent=employee.getParentId();

        System.out.println(empName+desg+parent);

        Designation designation=degRepo.findByDesgName(desg);
        float cLevel=designation.getLevel();

        Employee employee1=empRepo.findByEmpId(parent);
        float pLevel=employee1.designation.level;

        if(pLevel<cLevel)
        {
            Employee emp=new Employee(designation,parent,empName);
            empRepo.save(emp);
            status=HttpStatus.OK;
            res="Data Saved";
        }
        else
        {
            status=HttpStatus.BAD_REQUEST;
            res="Bad Request";
        }
        return new ResponseEntity<>(res,status);
    }


    @GetMapping("/home/{aid}")
    public ResponseEntity getUser(@PathVariable("aid") int aid)
    {
        Employee manager=null;
        List<Employee> colleagues=null;
        Map<String,Object> map=new LinkedHashMap<>();

        Employee emp=empRepo.findByEmpId(aid);
        if(emp!=null)
        {
            map.put("Employee",emp);

            if(emp.getParentId()!=null) {
                manager = empRepo.findByEmpId(emp.getParentId());
                map.put("Manager",manager);

                colleagues=empRepo.findAllByParentIdAndEmpNameIsNot(emp.getParentId(),emp.getEmpName());
                map.put("Colleagues",colleagues);
            }

            List<Employee> reporting=empRepo.findAllByParentIdAndEmpNameIsNot(emp.getEmpId(),emp.getEmpName());
            if(reporting.size()!=0)
                map.put("Reporting Too",reporting);

            return new ResponseEntity<>(map,HttpStatus.OK);
        }
        else
        {
            return new ResponseEntity<>("Bad Request",HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/home/{empId}")
    public String putData(@PathVariable("empId") int empId,@RequestBody EmployeePost emp)
    {
        String result="";
        if(emp.isReplace())
        {
            Integer parent=null;
            Employee employee=empRepo.findByEmpId(empId);
            if(employee!=null)
            {
                float oldLevel=employee.designation.getLevel();
                float currLevel=degRepo.findByDesgName(emp.getEmpDesg()).getLevel();
                if(oldLevel>=currLevel)
                {
                    parent=employee.getParentId();
                    empRepo.delete(employee);
                    Employee employee1=new Employee();
                    employee1.designation=degRepo.findByDesgName(emp.getEmpDesg());

                    employee1.setEmpName(emp.getEmpName());
                    employee1.setParentId(parent);
                    empRepo.save(employee1);
                    List<Employee> list=empRepo.findAllByParentId(empId);
                    for(int i=0;i<list.size();i++)
                    {
                        Employee empTemp=list.get(i);
                        empTemp.setParentId(employee1.getEmpId());
                        empRepo.save(empTemp);
                    }
                    result="Updated";
                }
                else
                {
                    result="Bad Request";
                }
            }
        }
        else
        {

            Employee employee = empRepo.findByEmpId(empId);
            if(employee!=null)
            {
                Integer parentID=emp.getParentId();
                String empDesg=emp.getEmpDesg();
                String empName=emp.getEmpName();

                if(parentID!=null) {
                    Employee employee1 = empRepo.findByEmpId(emp.getParentId());
                    float baap = employee1.designation.getLevel();
                    if (empDesg != null) {
                        float desgLevel = degRepo.findByDesgName(emp.getEmpDesg()).getLevel();
                        if (!(baap >= desgLevel)) {
                            employee.setParentId(parentID);
                            employee.designation=degRepo.findByDesgName(empDesg);
                            result="Updated";
                        }
                    }
                    else
                    {
                        float parentLevel=empRepo.findByEmpId(emp.getParentId()).designation.getLevel();
                        float selfLevel=empRepo.findByEmpId(empId).designation.getLevel();
                        if(parentLevel<selfLevel)
                        {
                            employee.setParentId(emp.getParentId());
                            result="Updated";
                        }
                        else
                        {
                            result="Bad Request";
                        }
                    }
                }
                    else if(empDesg!=null && parentID==null) {
                    if (empName != null) {
                        float currParent = empRepo.findByEmpId(employee.getParentId()).designation.getLevel();
                        float desgLevel = degRepo.findByDesgName(empDesg).getLevel();
                        if (!(currParent >= desgLevel)) {
                            employee.setDesgName(empDesg);
                            result="Updated";
                        }
                        else
                        {
                            result="Bad Request";
                        }
                    }
                }
                    else if(empName!=null)
                {
                        employee.setEmpName(empName);
                        result="Updated";
                }
                    else
                {
                    result="Bad Request";
                }
            }
            else
            {
                result="Bad Request";
            }

            empRepo.save(employee);

        }
        return result;
    }

    @DeleteMapping("/home/{eid}")
    public ResponseEntity deleteEmployee(@PathVariable("eid") int eid)
    {
        Employee emp=empRepo.findByEmpId(eid);
        if(emp!=null)
        {
            if(emp.getDesgName().equals("director"))
            {
                List<Employee> list=empRepo.findAllByParentId(emp.getEmpId());
                if(list.size()>0)
                {
                    // Not able to delete
                    return new ResponseEntity("Bad Request",HttpStatus.BAD_REQUEST);
                }
                else
                {
                    //Able to delete
                    empRepo.delete(emp);
                    return new ResponseEntity("Deleted Successfully",HttpStatus.OK);
                }
            }
            else
            {
                int parentId=emp.getParentId();
                List<Employee> childs=empRepo.findAllByParentId(emp.getEmpId());
                for(Employee employee:childs)
                {
                    employee.setParentId(parentId);
                    empRepo.save(employee);
                }
                empRepo.delete(emp);
                return new ResponseEntity("Deleted Successfully",HttpStatus.OK);
            }
        }
        else
        {
            return new ResponseEntity("Bad Request",HttpStatus.BAD_REQUEST);
        }
    }
}
