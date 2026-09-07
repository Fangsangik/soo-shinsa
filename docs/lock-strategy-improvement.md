# 락 전략 개선 방안

## 현재 문제점
- 분산락 + DB 락 이중 사용으로 인한 성능 저하 및 데드락 위험
- SERIALIZABLE 격리 수준으로 인한 과도한 락 대기

## 개선 방안

### Option 1: 분산락만 사용 (권장)
```java
@StockLock(key = "'lock:productOption:' + #productOptionId")
@Transactional(isolation = Isolation.READ_COMMITTED)
public OrdersResponseDto createOrder(User user, Long productOptionId, Integer quantity) {
    // DB 락 제거, 분산락으로만 동시성 제어
    ProductOption productOption = productOptionRepository.findById(productOptionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_PRODUCT_OPTION));
    
    // 비관적 락 대신 낙관적 락 또는 애플리케이션 레벨 검증
    if (productOption.getQuantity() < quantity) {
        throw new InvalidInputException(ErrorCode.CAN_NOT_USE_PRODUCT);
    }
    
    // 원자적 업데이트
    int updatedRows = productOptionRepository.decreaseStock(productOptionId, quantity);
    if (updatedRows == 0) {
        throw new InvalidInputException(ErrorCode.CAN_NOT_USE_PRODUCT);
    }
}
```

### Option 2: DB 락만 사용
```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public OrdersResponseDto createOrder(User user, Long productOptionId, Integer quantity) {
    // 분산락 제거, DB 락만 사용
    ProductOption productOption = productOptionRepository.findByIdWithLock(productOptionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_PRODUCT_OPTION));
    
    if (productOption.getQuantity() < quantity) {
        throw new InvalidInputException(ErrorCode.CAN_NOT_USE_PRODUCT);
    }
    
    productOption.decreaseQuantity(quantity);
    productOptionRepository.save(productOption);
}
```

### Option 3: 낙관적 락 사용
```java
@Version
private Long version;

@Transactional(isolation = Isolation.READ_COMMITTED)
public OrdersResponseDto createOrder(User user, Long productOptionId, Integer quantity) {
    int maxRetries = 3;
    int retryCount = 0;
    
    while (retryCount < maxRetries) {
        try {
            ProductOption productOption = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_PRODUCT_OPTION));
            
            if (productOption.getQuantity() < quantity) {
                throw new InvalidInputException(ErrorCode.CAN_NOT_USE_PRODUCT);
            }
            
            productOption.decreaseQuantity(quantity);
            productOptionRepository.save(productOption); // 낙관적 락 충돌 시 예외 발생
            break;
        } catch (OptimisticLockException e) {
            retryCount++;
            if (retryCount >= maxRetries) {
                throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            // 잠시 대기 후 재시도
            try {
                Thread.sleep(50 * retryCount);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }
    }
}
```

## 권장 사항
1. **분산락만 사용** - 여러 서버 인스턴스에서 안전한 동시성 제어
2. **격리 수준을 READ_COMMITTED로 변경** - 성능 향상
3. **원자적 업데이트 쿼리 사용** - 경쟁 조건 방지
4. **락 순서 일관성 유지** - 데드락 방지