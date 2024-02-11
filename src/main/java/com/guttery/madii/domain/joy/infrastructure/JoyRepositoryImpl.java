package com.guttery.madii.domain.joy.infrastructure;

import com.guttery.madii.common.util.OrderSpecifierUtils;
import com.guttery.madii.domain.joy.application.dto.JoyGetMyAllResponse;
import com.guttery.madii.domain.joy.application.dto.JoyGetMyOne;
import com.guttery.madii.domain.joy.application.dto.JoyGetRecommendRequest;
import com.guttery.madii.domain.joy.application.dto.JoyGetRecommendResponse;
import com.guttery.madii.domain.joy.domain.model.Joy;
import com.guttery.madii.domain.joy.domain.model.JoyType;
import com.guttery.madii.domain.joy.domain.model.QJoy;
import com.guttery.madii.domain.joy.domain.repository.JoyQueryDslRepository;
import com.guttery.madii.domain.joytag.domain.model.QJoyTag;
import com.guttery.madii.domain.tag.domain.model.QTag;
import com.guttery.madii.domain.tag.domain.model.TagType;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.guttery.madii.domain.joy.domain.model.QJoy.joy;
import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;


@Repository
@RequiredArgsConstructor
@Slf4j
public class JoyRepositoryImpl implements JoyQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<JoyGetMyAllResponse> getMyJoy(Long userId) {
        QJoy joy = QJoy.joy;
        QJoy joySub = new QJoy("joySub");

        StringTemplate formattedDate = Expressions.stringTemplate(
                "DATE_FORMAT({0}, {1})", joy.createdAt, "%Y. %m. %d"
        );
        StringTemplate formattedDateSub = Expressions.stringTemplate(
                "DATE_FORMAT({0}, {1})", joySub.createdAt, "%Y. %m. %d"
        );

        return queryFactory
                .selectFrom(joy)
                .where(joy.user.userId.eq(userId))
                .orderBy(joy.createdAt.desc())
                .transform(groupBy(formattedDate)
                        .list(Projections.constructor(JoyGetMyAllResponse.class,
                                formattedDate,
                                JPAExpressions.select(joySub.count())
                                        .from(joySub)
                                        .where(joySub.user.userId.eq(userId), formattedDateSub.eq(formattedDate)),
                                list(Projections.constructor(JoyGetMyOne.class,
                                        joy.joyId,
                                        joy.joyIconNum,
                                        joy.contents)))));
    }

    @Override
    public List<Joy> getRandomOfficialJoys(int amount) {
        final QJoy joy = QJoy.joy;

        return queryFactory
                .selectFrom(joy)
                .where(joy.joyType.eq(JoyType.OFFICIAL))
                .orderBy(OrderSpecifierUtils.makeRandom())
                .limit(amount)
                .fetch();
    }

    @Override
    public List<JoyGetRecommendResponse> getJoyRecommend(JoyGetRecommendRequest request) {
        // 태그 유형별로 조건을 만족하는 Joy ID 목록을 조회
        Set<Long> whenJoyIds = new HashSet<>(getJoyIdsByTagType(TagType.WHEN, request.when()));
        Set<Long> whoJoyIds = new HashSet<>(getJoyIdsByTagType(TagType.WHO, request.who()));
        Set<Long> whichJoyIds = new HashSet<>(getJoyIdsByTagType(TagType.WHICH, request.which()));

        // 교집합 찾기
        Set<Long> resultJoyIds = new HashSet<>(whenJoyIds);
        if (!whoJoyIds.isEmpty()) resultJoyIds.retainAll(whoJoyIds);
        if (!whichJoyIds.isEmpty()) resultJoyIds.retainAll(whichJoyIds);

        // 결과 Joy 객체 조회
        List<Joy> allRecommendations = queryFactory
                .selectFrom(QJoy.joy)
                .where(joy.joyId.in(resultJoyIds))
                .fetch();

        // 결과 목록을 무작위로 섞기
        Collections.shuffle(allRecommendations);

        // 최대 3개의 항목만 선택하여 DTO로 변환
        return allRecommendations.stream()
                .limit(3)
                .map(joy -> new JoyGetRecommendResponse(joy.getJoyId(), joy.getJoyIconNum(), joy.getContents()))
                .collect(Collectors.toList());
    }

    private List<Long> getJoyIdsByTagType(TagType tagType, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return Collections.emptyList();

        QJoyTag qJoyTag = QJoyTag.joyTag;
        QTag qTag = QTag.tag;

        return queryFactory
                .select(qJoyTag.joy.joyId)
                .from(qJoyTag)
                .join(qJoyTag.tag, qTag)
                .where(qTag.tagType.eq(tagType).and(qTag.tagId.in(tagIds)))
                .fetch();
    }

}
