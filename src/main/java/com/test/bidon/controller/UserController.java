package com.test.bidon.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.test.bidon.config.security.CustomUserDetails;
import com.test.bidon.dto.CombinedAuctionDTO;
import com.test.bidon.dto.CustomOAuth2User;
import com.test.bidon.dto.UserInfoDTO;
import com.test.bidon.entity.OneOnOne;
import com.test.bidon.entity.OneOnOneAnswer;
import com.test.bidon.entity.UserEntity;
import com.test.bidon.repository.NormalAuctionItemRepository;
import com.test.bidon.repository.OneOnOneAnswerRepository;
import com.test.bidon.repository.OneOnOneRepository;
import com.test.bidon.repository.UserRepository;
import com.test.bidon.service.BidOrderService;
import com.test.bidon.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final OneOnOneRepository oneOnOneRepository;
    private final OneOnOneAnswerRepository oneOnOneAnswerRepository;
    private final NormalAuctionItemRepository normalAuctionItemRepository;
    private final BidOrderService bidOrderService;
    
    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("userInfoDTO", new UserInfoDTO());
        return "user/signup";
    }

    @PostMapping("/signok")
    @ResponseBody
    public ResponseEntity<Map<String, String>> signupok(
            @RequestPart(name = "userInfoDTO") UserInfoDTO userInfoDTO,
            @RequestPart(name = "profileFile", required = false) MultipartFile profileFile) {
        try {
            log.info("회원가입 요청 데이터: {}", userInfoDTO);

            userInfoDTO.setDefaultValues();

            if (userService.isEmailExists(userInfoDTO.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "이미 사용 중인 이메일입니다."));
            }

            if (profileFile != null && !profileFile.isEmpty()) {
                try {
                    String savedFileName = saveProfileFile(profileFile);
                    userInfoDTO.setProfile(savedFileName);
                } catch (IOException e) {
                    log.error("프로필 이미지 저장 중 오류: ", e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("message", "프로필 이미지 저장 중 오류가 발생했습니다."));
                }
            } else {
                userInfoDTO.setProfile("default-profile.svg");
            }

            UserEntity savedUser = userService.registerUser(userInfoDTO);

            return ResponseEntity.ok(Map.of("message", "회원가입이 완료되었습니다."));
        } catch (Exception e) {
            log.error("회원가입 처리 중 오류: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "회원가입 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    private String saveProfileFile(MultipartFile file) throws IOException {
    	
    	if (file.isEmpty()) {
            return "default-profile.svg";
        }

        String uploadDir = "src/main/resources/static/uploads/profiles/";
        
        // 디렉토리가 없으면 생성
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        // 파일 저장
        Path path = Paths.get(uploadDir + fileName);
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "user/login";
    }
    
    @GetMapping("/logout")
	public String logout(HttpServletRequest request, HttpServletResponse response) throws Exception {		
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		if (authentication != null) {
			new SecurityContextLogoutHandler().logout(request, response, authentication);	//로그아웃
		}
		
		return "redirect:/";
	}

    

    @GetMapping("/mypage")
    @PreAuthorize("isAuthenticated()")
    public String mypage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        
        if (auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails customUserDetails = (CustomUserDetails) auth.getPrincipal();
            user = userRepository.findById(customUserDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        } else if (auth.getPrincipal() instanceof CustomOAuth2User) {
            CustomOAuth2User oauth2User = (CustomOAuth2User) auth.getPrincipal();
            user = userRepository.findByEmail(oauth2User.getEmail());
        }

        if (user != null) {
        
            //CustomUserDetails 대신 DB에서 가져온 최신 user 정보를 사용
            model.addAttribute("name", user.getName());
            model.addAttribute("email", user.getEmail());
            model.addAttribute("national", user.getNational());
            model.addAttribute("birth", user.getBirth());
            model.addAttribute("tel", user.getTel());
            model.addAttribute("createDate", user.getCreateDate());
            model.addAttribute("profile", user.getProfile());
            model.addAttribute("provider", user.getProvider());
            model.addAttribute("biddingCount", bidOrderService.countBiddingActivities(user.getId()));
            model.addAttribute("wonCount", bidOrderService.countWonAuctions(user.getId()));
            model.addAttribute("sellingCount", bidOrderService.countSellingActivities(user.getId()));
            
            //관리자 문의
            //OneOnOne 데이터 가져오기
//            List<OneOnOne> oneOnOneList = oneOnOneRepository.findByUserEntityInfo(user);
//            
//            //각 OneOnOne에 대한 답변을 가져와서 설정
//            for (OneOnOne oneOnOne : oneOnOneList) {
//                OneOnOneAnswer answer = oneOnOneAnswerRepository.findByOneOnOneId(null);
//                if (answer != null) {
//                    oneOnOne.setOneOnOneAnswer(answer);
//                }
//            }
//            
//            model.addAttribute("oneOnOneList", oneOnOneList);
//            
            //나의 활동 > 일반 경매와 실시간 경매 통합 처리
            List<CombinedAuctionDTO> normalAuctions = normalAuctionItemRepository.findByUserInfoId(user.getId())
                .stream()
                .map(item -> {
                    Duration duration = Duration.between(item.getStartTime(), item.getEndTime());
                    long secondsRemaining = duration.getSeconds();
                    String remainingTime = formatRemainingTime(secondsRemaining);

                    return CombinedAuctionDTO.builder()
                        .auctionType("일반")
                        .id(item.getId())
                        .name(item.getName())
                        .currentPrice(bidOrderService.getFinalPriceByNormalBidId(item.getId()))
                        .startPrice(item.getStartPrice())
                        .remainingTime(remainingTime)
                        .build();
                })
                .collect(Collectors.toList());

            List<CombinedAuctionDTO> liveAuctions = bidOrderService.getLiveBidsByUserId(user.getId())
            	    .stream()
            	    .map(item -> {
            	        Duration duration = Duration.between(LocalDateTime.now(), item.getEndTime());
            	        String remainingTime = formatRemainingTime(duration.getSeconds());

            	        return CombinedAuctionDTO.builder()
            	            .auctionType("실시간")
            	            .id(item.getId())
            	            .name(item.getName())
            	            .currentPrice(item.getFinalPrice())
            	            .startPrice(item.getStartPrice())
            	            .remainingTime(remainingTime)
            	            .build();
            	    })
            	    .collect(Collectors.toList());

            // 두 리스트 병합
            List<CombinedAuctionDTO> combinedAuctions = new ArrayList<>();
            combinedAuctions.addAll(normalAuctions);
            combinedAuctions.addAll(liveAuctions);
            
            // 입찰한 경매 목록
            model.addAttribute("combinedAuctions", bidOrderService.getBiddingAuctions(user.getId()));
            
            // 낙찰받은 경매 목록
            model.addAttribute("wonAuctions", bidOrderService.getWonAuctions(user.getId()));
            
            // 판매된 경매 목록
            model.addAttribute("soldAuctions", bidOrderService.getSoldAuctionsByUserId(user.getId()));
        
            
            
        }
        
		
            
        return "user/mypage";
    }
    
    //남은 시간 포맷팅 메서드 > 따로 빼서 관리
    private String formatRemainingTime(long secondsRemaining) {
        long days = secondsRemaining / (24 * 3600);
        long hours = (secondsRemaining % (24 * 3600)) / 3600;
        long minutes = (secondsRemaining % 3600) / 60;
        long seconds = secondsRemaining % 60;
        
        return String.format("%d일 %02d:%02d:%02d", days, hours, minutes, seconds);
    }
    
    @PostMapping("/api/user/update")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUser(
        @RequestParam(value = "name", required = false) String name,
        @RequestParam(value = "password", required = false) String password,
        @RequestParam(value = "birth", required = false) String birth,
        @RequestParam(value = "national", required = false) String national,
        @RequestParam(value = "tel", required = false) String tel,
        @RequestParam(value = "profile", required = false) MultipartFile profile) {
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserEntity user = null;
            
            if (auth.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            } else if (auth.getPrincipal() instanceof CustomOAuth2User) {
                CustomOAuth2User oauth2User = (CustomOAuth2User) auth.getPrincipal();
                user = userRepository.findByEmail(oauth2User.getEmail());
            }
            
            if (user == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", "사용자를 찾을 수 없습니다."
                    ));
            }

            // OAuth2 사용자의 경우 비밀번호 수정 방지
            if ("google".equals(user.getProvider()) && password != null) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", "소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다."
                    ));
            }
            
            UserInfoDTO updateDto = new UserInfoDTO();
            updateDto.setName(name);
            updateDto.setPassword(password);
            if (birth != null && !birth.isEmpty()) {
                updateDto.setBirth(LocalDate.parse(birth));
            }
            updateDto.setNational(national);
            updateDto.setTel(tel);
            
            if (profile != null && !profile.isEmpty()) {
                String savedFileName = saveProfileFile(profile);
                updateDto.setProfile(savedFileName);
            }
            
            UserEntity updatedUser = userService.updateUser(user.getId(), updateDto);
            
            return ResponseEntity.ok()
                .body(Map.of(
                    "success", true,
                    "message", "정보가 성공적으로 수정되었습니다."
                ));
                
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "message", e.getMessage()
                ));
        }
    }
    
    
}