package com.kafkaproducer.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Customer {

	private int id;
	private String name;
	private String email;
	private String contactNo;

}