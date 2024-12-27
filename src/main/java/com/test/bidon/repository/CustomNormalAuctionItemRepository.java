package com.test.bidon.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;

import com.querydsl.core.types.Projections;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.test.bidon.dto.NormalAuctionWishDTO;
import com.test.bidon.entity.NormalAuctionItem;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.test.bidon.entity.QNormalAuctionItem.normalAuctionItem;
import static com.test.bidon.entity.QNormalAuctionItemImage.normalAuctionItemImage;
import static com.test.bidon.entity.QNormalAuctionItemImageList.normalAuctionItemImageList;
import static com.test.bidon.entity.QNormalBidInfo.normalBidInfo;
import static com.test.bidon.entity.QNormalAuctionWish.normalAuctionWish;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.test.bidon.entity.NormalAuctionItem;

import lombok.RequiredArgsConstructor;

//Query DSL용 Repository
@Repository
@RequiredArgsConstructor
public class CustomNormalAuctionItemRepository {

    private final JPAQueryFactory jpaQueryFactory;
    /*
        boot-jpa/CustomAddressRepository.java 참고
     */

    // 대표이미지 값이 1이고, 경매 상태가 진행중인 tuple만 골라서 browse-bid 페이지에 출력하는 method
    // offset과 limit으로 Controller에서 페이징 구현
    public List<Tuple> joinTablesToDisplayBrowseBid(int offset, int limit) {

        return jpaQueryFactory
                .select(normalAuctionItem.id, normalAuctionItem.name, normalAuctionItemImage.path, normalAuctionItem.startPrice)
                .from(normalAuctionItemImageList)
                .join(normalAuctionItemImageList.normalAuctionItem, normalAuctionItem)
                .join(normalAuctionItemImageList.normalAuctionItemImage, normalAuctionItemImage)
                .where(normalAuctionItemImageList.isMainImage.eq(1L)
                        .and(normalAuctionItem.status.eq("진행중"))
                )
                .orderBy(normalAuctionItem.id.asc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    // 경매 시작가 범위 필터가 적용된 상태의 데이터 출력 method
    public List<Tuple> joinTablesToDisplayBrowseBidRangedByStartPrice(int offset, int limit, int lower, int upper) {

        return jpaQueryFactory
                .select(normalAuctionItem.id, normalAuctionItem.name, normalAuctionItemImage.path, normalAuctionItem.startPrice)
                .from(normalAuctionItemImageList)
                .join(normalAuctionItemImageList.normalAuctionItem, normalAuctionItem)
                .join(normalAuctionItemImageList.normalAuctionItemImage, normalAuctionItemImage)
                .where(normalAuctionItemImageList.isMainImage.eq(1L)
                        .and(normalAuctionItem.status.eq("진행중"))
                        .and(normalAuctionItem.startPrice.between(lower, upper))
                )
                .orderBy(normalAuctionItem.id.asc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    // List<Tuple>의 Tuple 개수를 세는 method
    public long count(int offset, int limit) {

        return jpaQueryFactory
                .select(normalAuctionItem.id, normalAuctionItem.name, normalAuctionItemImage.path)
                .from(normalAuctionItemImageList)
                .join(normalAuctionItemImageList.normalAuctionItem, normalAuctionItem)
                .join(normalAuctionItemImageList.normalAuctionItemImage, normalAuctionItemImage)
                .where(normalAuctionItemImageList.isMainImage.eq(1L)
                        .and(normalAuctionItem.status.eq("진행중"))
                )
                .orderBy(normalAuctionItem.id.asc())
                .offset(offset)
                .limit(limit)
                .fetchCount();
    }

    // 경매 시작가 범위 필터가 적용된 상태의 데이터 출력 개수 카운트 method
    public long countRangedByStartPrice(int offset, int limit, int lower, int upper) {

        return jpaQueryFactory
                .select(normalAuctionItem.id, normalAuctionItem.name, normalAuctionItemImage.path)
                .from(normalAuctionItemImageList)
                .join(normalAuctionItemImageList.normalAuctionItem, normalAuctionItem)
                .join(normalAuctionItemImageList.normalAuctionItemImage, normalAuctionItemImage)
                .where(normalAuctionItemImageList.isMainImage.eq(1L)
                        .and(normalAuctionItem.status.eq("진행중"))
                        .and(normalAuctionItem.startPrice.between(lower, upper))
                )
                .orderBy(normalAuctionItem.id.asc())
                .offset(offset)
                .limit(limit)
                .fetchCount();
    }

    // 경매 물품의 종료 시간과 현재 최고 입찰가를 불러오는 method
    public List<Tuple> getTimeAndPrice() {
                /*
                    SELECT auc.id, auc.name, auc.endTime, MAX(bidPrice) AS maxPrice
                    FROM NormalAuctionItem auc
                    INNER JOIN NormalBidInfo bid
                        ON bid.auctionItemId = auc.id
                    GROUP BY auc.id, auc.name, auc.endTime;
                    ...를 Query DSL 방식으로 refactoring 필요.
                */

        return jpaQueryFactory
                .select(normalAuctionItem.id, normalAuctionItem.endTime, normalBidInfo.bidPrice.max())
                .from(normalBidInfo)
                .join(normalBidInfo.normalAuctionItem, normalAuctionItem)
                .where(normalAuctionItem.status.eq("진행중"))
                .groupBy(normalAuctionItem.id, normalAuctionItem.name, normalAuctionItem.endTime)
                .orderBy(normalAuctionItem.id.asc())
                .fetch();
    }

    public List<Tuple> getTimeAndPriceRangedByStartPrice(int lower, int upper) {

        return jpaQueryFactory
                .select(normalAuctionItem.id, normalAuctionItem.endTime, normalBidInfo.bidPrice.max())
                .from(normalBidInfo)
                .join(normalBidInfo.normalAuctionItem, normalAuctionItem)
                .where(normalAuctionItem.status.eq("진행중")
                        .and(normalAuctionItem.startPrice.between(lower, upper)))
                .groupBy(normalAuctionItem.id, normalAuctionItem.name, normalAuctionItem.endTime)
                .orderBy(normalAuctionItem.id.asc())
                .fetch();
    }

    // 최근 경매 물품 6개를 출력하기 위한 method
    public List<Tuple> DisplaySixItems() {

        return jpaQueryFactory
                .select(normalAuctionItem.id, normalAuctionItem.name, normalAuctionItemImage.path, normalAuctionItem.description)
                .from(normalAuctionItemImageList)
                .join(normalAuctionItemImageList.normalAuctionItem, normalAuctionItem)
                .join(normalAuctionItemImageList.normalAuctionItemImage, normalAuctionItemImage)
                .where(normalAuctionItemImageList.isMainImage.eq(1L)
                        .and(normalAuctionItem.status.eq("진행중"))
                )
                .orderBy(normalAuctionItem.startTime.desc())
                .limit(6) // 처음 6개로 데이터 제한
                .fetch();
    }

    // 최근 경매 물품 6개의 경매 종료 시간, 현재 경매가를 출력하기 위한 method
    public List<Tuple> DisplaySixTimeAndPrice() {

        return jpaQueryFactory
                .select(normalAuctionItem.id, normalAuctionItem.endTime, normalBidInfo.bidPrice.max())
                .from(normalBidInfo)
                .join(normalBidInfo.normalAuctionItem, normalAuctionItem)
                .where(normalAuctionItem.status.eq("진행중"))
                .groupBy(normalAuctionItem.id, normalAuctionItem.name, normalAuctionItem.endTime)
                .orderBy(normalAuctionItem.id.asc())
                .limit(6)
                .fetch();
    }

    // AdminDashboardController.java에서 등록된 일반 경매 물품의 총 개수를 계산할 때 사용하는 method
    public Long count() {

        return jpaQueryFactory
                .selectFrom(normalAuctionItem)
                .fetchCount();
    }

    // TestAdminController.java에서 일반 경매 물품들의 시작시간을 내림차순으로 구할 때 사용하는 method
    public List<NormalAuctionItem> findAll(Sort startTime) {

        return jpaQueryFactory
                .selectFrom(normalAuctionItem)
                .fetch();

    }

    // 위시리스트 수 조회
    public List<NormalAuctionWishDTO> getWishCountStats() {
    	
    	LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
    	
        return jpaQueryFactory
            .select(Projections.constructor(NormalAuctionWishDTO.class,
            	normalAuctionItem.id,
                normalAuctionItem.name,            // 경매 물품 이름
                normalAuctionItem.startTime,       // 경매 시작 시간
                normalAuctionWish.id.count()       // 찜 수 (찜한 사용자 수)
            ))
            .from(normalAuctionItem)
            .leftJoin(normalAuctionWish) 
            .on(normalAuctionItem.id.eq(normalAuctionWish.normalAuctionItemId)) 
            .groupBy(normalAuctionItem.id, normalAuctionItem.name, normalAuctionItem.startTime) 
            .where(normalAuctionItem.startTime.after(oneMonthAgo))
            .having(normalAuctionWish.id.count().gt(0))  
            .orderBy(normalAuctionWish.id.count().desc())  
            .fetch();  // 결과 반환
    }


    public OrderSpecifier<?> itemFilter(String filterTrigger) {

        // 필터가 적용되지 않으면
        if (filterTrigger == null || filterTrigger.isEmpty()) {
            return normalAuctionItem.id.asc();
        }

        switch (filterTrigger) {
            case "priceAsc":
                return normalAuctionItem.startPrice.asc();
            case "priceDesc":
                return normalAuctionItem.startPrice.desc();
            default:
                return normalAuctionItem.id.asc();

        }

    }

}