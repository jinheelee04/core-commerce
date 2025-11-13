package com.hhplus.ecommerce.domain.product.repository;

import com.hhplus.ecommerce.domain.product.entity.Product;
import com.hhplus.ecommerce.domain.product.entity.ProductStatus;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.ArrayList;
import java.util.List;

import static com.hhplus.ecommerce.domain.product.entity.QProduct.product;

@RequiredArgsConstructor
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private static final String SORT_BY_VIEWS = "views";
    private static final String SORT_BY_SALES = "sales";

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Product> findByDynamicFilters(Long categoryId, ProductStatus status, Pageable pageable) {
        JPAQuery<Product> query = queryFactory
                .selectFrom(product)
                .where(
                        categoryIdEq(categoryId),
                        statusEq(status)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        for (OrderSpecifier<?> orderSpecifier : getOrderSpecifiers(pageable)) {
            query.orderBy(orderSpecifier);
        }

        List<Product> content = query.fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(product.count())
                .from(product)
                .where(
                        categoryIdEq(categoryId),
                        statusEq(status)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<Product> findPopularProducts(String sortBy, Pageable pageable) {
        JPAQuery<Product> query = queryFactory
                .selectFrom(product)
                .where(product.status.eq(ProductStatus.ACTIVE))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        if (SORT_BY_VIEWS.equals(sortBy)) {
            query.orderBy(product.viewCount.desc());
        } else if (SORT_BY_SALES.equals(sortBy)) {
            query.orderBy(product.salesCount.desc());
        } else {
            query.orderBy(product.viewCount.desc());
        }

        List<Product> content = query.fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(product.count())
                .from(product)
                .where(product.status.eq(ProductStatus.ACTIVE));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private List<OrderSpecifier<?>> getOrderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        if (pageable.getSort().isEmpty()) {
            orders.add(product.createdAt.desc());
            return orders;
        }

        for (Sort.Order sortOrder : pageable.getSort()) {
            Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;
            PathBuilder<Product> pathBuilder = new PathBuilder<>(Product.class, "product");
            orders.add(new OrderSpecifier(direction, pathBuilder.get(sortOrder.getProperty())));
        }

        return orders;
    }

    private BooleanExpression categoryIdEq(Long categoryId) {
        return categoryId != null ? product.category.id.eq(categoryId) : null;
    }

    private BooleanExpression statusEq(ProductStatus status) {
        return status != null ? product.status.eq(status) : null;
    }
}
