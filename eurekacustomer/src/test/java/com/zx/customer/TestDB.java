package com.zx.customer;

import com.zx.customer.service.ApiService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest(classes = CustomerApplication.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TestDB {

    @Autowired
    private ApiService apiService;

    @Test
    public void test(){
        try {
            System.out.println("===============================");
            System.out.println(apiService.index());
            System.out.println("===============================");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}