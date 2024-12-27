package com.test.bidon.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.test.bidon.dto.MonthlyAvgtStartBidPriceDTO;
import com.test.bidon.dto.MonthlyItemCountDTO;
import com.test.bidon.dto.MonthlyRevenueDTO;
import com.test.bidon.dto.MonthlyUserCountDTO;
import com.test.bidon.dto.QuarterlyRevenueDTO;
import com.test.bidon.dto.SubCategoryCountDTO;
import com.test.bidon.repository.CustomAdminDashboardRepository;
import com.test.bidon.repository.CustomNormalAuctionItemRepository;
import com.test.bidon.repository.LiveAuctionItemRepository;
import com.test.bidon.repository.UserRepository;

@Controller
public class AdminDashboardController {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private CustomAdminDashboardRepository customAdminDashboardRepository;
	@Autowired
	private CustomNormalAuctionItemRepository customNormalAuctionItemRepository;
	@Autowired
	private LiveAuctionItemRepository liveAuctionItemRepository;

	@GetMapping("/admin")
	public String index(Model model) {

		// 총회원수
		long userCount = userRepository.count();
		// 30일 이내 가입한 신규 인원 수
		long newUserCount = customAdminDashboardRepository.newUserCount();
		// 월별 누적 회원 수 + 월별 신규 회원 수
		List<MonthlyUserCountDTO> monthlyUserCountList = customAdminDashboardRepository.findMonthlyUserCounts();

		List<Long> monthlyNewUserCounts = new ArrayList<>(Collections.nCopies(12, 0L));
		List<Long> monthlyExistingUserCounts = new ArrayList<>(Collections.nCopies(12, 0L));

		for (MonthlyUserCountDTO dto : monthlyUserCountList) {
			int monthIndex = Integer.parseInt(dto.getMonth()) - 1;
			monthlyNewUserCounts.set(monthIndex, dto.getCount());
			monthlyExistingUserCounts.set(monthIndex, dto.getCumulativeCount());
		}


		// 경매 참여자 수
		long bidEnterUserCount = customAdminDashboardRepository.getBidEnterUserCount();

		// 일반 경매 상품 수
		long normalItemCount = customNormalAuctionItemRepository.count();
		// 실시간 경매 상품 수
		long liveItemCount = liveAuctionItemRepository.count();
		// 총 수익률
		long totalBidPrice = customAdminDashboardRepository.getTotalBidPrice();
		// 진행 중인 일반 경매 물품 수
		long ongoingNormalItemCount = customAdminDashboardRepository.getOngoingNormalAuctionItemCount();

		// 진행 중인 실시간 경매 물품 수
		long ongoingLiveItemCount = customAdminDashboardRepository.getOngoingLiveAuctionItemCount();

		// 총 낙찰 물품 수
		long totalWinningBidCount = customAdminDashboardRepository.getTotalWinningBidCount();

		// 월 평균 낙찰 가격
		Double averageBidPrice = customAdminDashboardRepository.getAverageBidPrice();

		// 월별 경매 물품 분석 리스트(진행중,종료된,등록된 경매)
		List<MonthlyItemCountDTO> monthlyItemCountList = customAdminDashboardRepository.findMonthlyItemCountList();

		List<Long> registeredCount = new ArrayList<>(Collections.nCopies(12, 0L));
		List<Long> ongoingCount = new ArrayList<>(Collections.nCopies(12, 0L));
		List<Long> endCount = new ArrayList<>(Collections.nCopies(12, 0L));

		for (MonthlyItemCountDTO dto : monthlyItemCountList) {
			try {
				int monthIndex = Integer.parseInt(dto.getMonth()) - 1;
				if (monthIndex >= 0 && monthIndex < 12) {
					registeredCount.set(monthIndex, dto.getRegisteredCount());
					ongoingCount.set(monthIndex, dto.getOngoingCount());
					endCount.set(monthIndex, dto.getEndCount());
				} else {
					System.err.println("Invalid month index: " + monthIndex);
				}
			} catch (NumberFormatException e) {
				System.err.println("Invalid month value: " + dto.getMonth());
			}
		}

		// 월별 경매 수수료 수익
		List<MonthlyRevenueDTO> monthlyRevenueList = customAdminDashboardRepository.getMonthlyRevenueList();
		List<Long> totalRevenue = new ArrayList<>(Collections.nCopies(12, 0L));
		List<Long> cumulativeTotalRevenue = new ArrayList<>(Collections.nCopies(12, 0L));
		for (MonthlyRevenueDTO dto : monthlyRevenueList) {
			int monthIndex = Integer.parseInt(dto.getMonth()) - 1;
			if (monthIndex >= 0 && monthIndex < 12) {
				totalRevenue.set(monthIndex, dto.getTotalRevenue());
				cumulativeTotalRevenue.set(monthIndex, dto.getCumulativeTotalRevenue());
			}
		}

		// 경매 성과 분석---------------------------------------------------------
		//월별 평균시작금액 + 평균낙찰금액
	    List<MonthlyAvgtStartBidPriceDTO> monthlyAvgtStartBidPriceList = customAdminDashboardRepository.getMonthlyAvgtStartBidPriceList();
	    List<Double> monthlyAverageStartPrice = new ArrayList<>(Collections.nCopies(12, 0.0));
	    List<Double> monthlyAverageBidPrice = new ArrayList<>(Collections.nCopies(12, 0.0));
	    for (MonthlyAvgtStartBidPriceDTO dto : monthlyAvgtStartBidPriceList) {
			int monthIndex = Integer.parseInt(dto.getMonth()) - 1;
			if (monthIndex >= 0 && monthIndex < 12) {
				monthlyAverageStartPrice.set(monthIndex, dto.getAvgStartPrice());
				monthlyAverageBidPrice.set(monthIndex, dto.getAvgBidPrice());
			}
		}

		// 카테고리별 물품 count
		List<SubCategoryCountDTO> subCategoryList = customAdminDashboardRepository.findTop4SubCategories();

		List<String> categoryNames = new ArrayList<>(Collections.nCopies(4, ""));
		List<Long> categoryCounts = new ArrayList<>(Collections.nCopies(4, 0L));

		for (int i = 0; i < subCategoryList.size(); i++) {
			SubCategoryCountDTO dto = subCategoryList.get(i);
			categoryNames.set(i, dto.getName());
			categoryCounts.set(i, dto.getCount());
		}


		// 분기별 일반, 실시간 경매 수익률
		List<QuarterlyRevenueDTO> quarterlyRevenue = customAdminDashboardRepository.getQuarterlyRevenue();

		// 분기별 데이터를 담을 리스트 초기화
		List<Double> quarterlyNormalRevenue = new ArrayList<>(Collections.nCopies(4, 0.0));
		List<Double> quarterlyLiveRevenue = new ArrayList<>(Collections.nCopies(4, 0.0));

		// DTO에서 데이터 추출하여 리스트에 저장
		for (QuarterlyRevenueDTO dto : quarterlyRevenue) {
			int quarterIndex = dto.getQuarter() - 1;
			if (quarterIndex >= 0 && quarterIndex < 4) {
				quarterlyNormalRevenue.set(quarterIndex, dto.getNormalRevenue());
				quarterlyLiveRevenue.set(quarterIndex, dto.getLiveRevenue());
			}
		}
		
		

		// 총 회원 수
		model.addAttribute("userCount", userCount);
		// 30일 이내 가입한 신규 회원 수
		model.addAttribute("newUserCount", newUserCount);
		// 월별 누적 회원 수
		model.addAttribute("monthlyNewUserCounts", monthlyNewUserCounts);
		// 월별 신규 회원 수
		model.addAttribute("monthlyExistingUserCounts", monthlyExistingUserCounts);
		// 월별 누적회원수 + 신규 회원 수 리스트
		model.addAttribute("monthlyUserCountList", monthlyUserCountList);
		// 경매 참여자수
		model.addAttribute("bidEnterUserCount", bidEnterUserCount);
		// 일반 경매 상품 수
		model.addAttribute("normalItemCount", normalItemCount);
		// 실시간 경매 상품 수
		model.addAttribute("liveItemCount", liveItemCount);
		//
		model.addAttribute("totalBidPrice", totalBidPrice);
		// 진행중인 일반 경매 물품수
		model.addAttribute("ongoingNormalItemCount", ongoingNormalItemCount);
		// 진행중인 실시간 경매 물품수
		model.addAttribute("ongoingLiveItemCount", ongoingLiveItemCount);
		// 총 낙찰 물품수
		model.addAttribute("totalWinningBidCount", totalWinningBidCount);
		// 총 평균 낙찰 가격
		model.addAttribute("averageBidPrice", averageBidPrice);
		// 월별 경매 물품 분석 리스트(진행중,종료된,등록된 경매)
		model.addAttribute("monthlyItemCountList", monthlyItemCountList);
		model.addAttribute("registeredCount", registeredCount);
		model.addAttribute("ongoingCount", ongoingCount);
		model.addAttribute("endCount", endCount);

		// 월별 경매 수수료 수익
		model.addAttribute("totalRevenue", totalRevenue);
		model.addAttribute("cumulativeTotalRevenue", cumulativeTotalRevenue);
		// 월별 평균 시작 가격
		model.addAttribute("monthlyAverageStartPrice", monthlyAverageStartPrice);
		// 월별 평균 낙찰 가격
		model.addAttribute("monthlyAverageBidPrice", monthlyAverageBidPrice);
		
		//서브 카테고리별 count 많은 순으로 4개만 가져오기
		model.addAttribute("categoryNames", categoryNames);
		model.addAttribute("categoryCounts", categoryCounts);

		//분기별 일반,실시간 수익
		model.addAttribute("quarterlyNormalRevenue", quarterlyNormalRevenue);
		model.addAttribute("quarterlyLiveRevenue", quarterlyLiveRevenue);

        
		return "admin/index";
	}

}
