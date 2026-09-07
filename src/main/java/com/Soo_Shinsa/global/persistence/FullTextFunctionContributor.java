package com.Soo_Shinsa.global.persistence;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

/**
 * JPQL/QueryDSL 에서 MySQL FULLTEXT 검색을 쓸 수 있게 함수를 등록한다.
 *
 * 자동완성은 네이티브 쿼리로 전환했지만 통합 검색은 QueryDSL 로 조립되므로
 * MATCH ... AGAINST 를 표현할 방법이 필요하다.
 *
 * 사용: function('match_against', 컬럼, 검색어) > 0
 */
public class FullTextFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().registerPattern(
                "match_against",
                "match(?1) against (?2 in boolean mode)",
                functionContributions.getTypeConfiguration()
                        .getBasicTypeRegistry()
                        .resolve(StandardBasicTypes.DOUBLE));
    }
}
