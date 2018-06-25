package jj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringStart
{
    public static void main(String[] args){
        System.out.println("Main Start");
        SpringApplication.run(SpringStart.class, args);
    }
}
