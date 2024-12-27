package com.test.bidon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyAvgtStartBidPriceDTO {
	private String month;
    private Double avgStartPrice;
    private Double avgBidPrice;
}
