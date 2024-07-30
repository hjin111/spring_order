package beyond.orderSystem.ordering.service;

import beyond.orderSystem.member.domain.Member;
import beyond.orderSystem.member.repository.MemberRepository;
import beyond.orderSystem.ordering.domain.OrderDetail;
import beyond.orderSystem.ordering.domain.OrderStatus;
import beyond.orderSystem.ordering.domain.Ordering;
import beyond.orderSystem.ordering.dto.OrderDeleteResDto;
import beyond.orderSystem.ordering.dto.OrderListResDto;
import beyond.orderSystem.ordering.dto.OrderSaveReqDto;
import beyond.orderSystem.ordering.repository.OrderDetailRepository;
import beyond.orderSystem.ordering.repository.OrderingRepository;
import beyond.orderSystem.product.domain.Product;
import beyond.orderSystem.product.repository.ProductRepository;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderingService {

    private final OrderingRepository orderingRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OrderDetailRepository orderDetailRepository;

    @Autowired
    public OrderingService(OrderingRepository orderingRepository, MemberRepository memberRepository, ProductRepository productRepository, OrderDetailRepository orderDetailRepository) {
        this.orderingRepository = orderingRepository;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.orderDetailRepository = orderDetailRepository;
    }

//    public Ordering orderCreate(OrderSaveReqDto dto){

//        //        방법1.쉬운방식
////        Ordering생성 : member_id, status
//        Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(()->new EntityNotFoundException("없음"));
//        Ordering ordering = orderingRepository.save(dto.toEntity(member));
//
////        OrderDetail생성 : order_id, product_id, quantity
//        for(OrderSaveReqDto.OrderDto orderDto : dto.getOrderDtos()){ //  dto.getOrderDtos() 이게  [{ productId : 1 , productCount : 2 } , {  productId : 2, productCount : 3 }] 이 리스트이다.
//            Product product = productRepository.findById(orderDto.getProductId()).orElse(null);
//            int quantity = orderDto.getProductCount();
//            OrderDetail orderDetail =  OrderDetail.builder()
//                    .product(product)
//                    .quantity(quantity)
//                    .ordering(ordering)
//                    .build();
//            orderDetailRepository.save(orderDetail);
//        }
//
//        return ordering;

        // 방법2. JPA에 최적화된 방식
//        Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(() -> new EntityNotFoundException("member is not found"));
//
//        Ordering ordering = Ordering.builder()
//                .member(member)
//                .build();
//
//        List<OrderDetail> orderDetailList = new ArrayList<>();
//        for(OrderSaveReqDto.OrderDto orderDetailDto : dto.getOrderDetailDtoList()){
//            Product product = productRepository.findById(orderDetailDto.getProductId()).orElseThrow(() -> new EntityNotFoundException("product is not found"));
//            OrderDetail orderDetail = OrderDetail.builder()
//                    .ordering(ordering)
//                    .product(product)
//                    .quantity(orderDetailDto.getProductCount())
//                    .build();
//
//            ordering.getOrderDetails().add(orderDetail); // ordering.getOrderDetails() 이거 List임
//        }
//
//        Ordering savedOrdering = orderRepository.save(ordering);
//        return savedOrdering;


        // 방법2. JPA에 최적화된 방식
//        Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(()->new EntityNotFoundException("없음"));
//        Ordering ordering = Ordering.builder()
//                .member(member)
//                .build();
//
//        for(OrderSaveReqDto.OrderDto orderDto : dto.getOrderDtos()){ //  dto.getOrderDtos() 이게  [{ productId : 1 , productCount : 2 } , {  productId : 2, productCount : 3 }] 이 리스트이다.
//            Product product = productRepository.findById(orderDto.getProductId()).orElse(null);
//            int quantity = orderDto.getProductCount();
//            OrderDetail orderDetail =  OrderDetail.builder()
//                    .product(product)
//                    .quantity(quantity)
//                    .ordering(ordering)
//                    .build();
//
//            ordering.getOrderDetails().add(orderDetail);
//
//        }
//
//        Ordering savedOrdering = orderingRepository.save(ordering);
//        return savedOrdering;
//
//    }

    public Ordering orderCreate(List<OrderSaveReqDto> dtos){
        // Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(()-> new EntityNotFoundException("member is not found"));

        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName(); // 이메일을 꺼내기
        Member member = memberRepository.findByEmail(memberEmail).orElseThrow(() -> new EntityNotFoundException("member is not found"));

        Ordering ordering = Ordering.builder()
                .member(member)
                .build();

        for(OrderSaveReqDto dto : dtos){
            Product product = productRepository.findById(dto.getProductId()).orElseThrow(()-> new EntityNotFoundException("product is not found"));
            int quantity = dto.getProductCount();
            if(quantity > product.getStockQuantity()){
                throw new IllegalArgumentException(product.getName() + "의 재고가 부족합니다. 현재 재고: " + product.getStockQuantity());
            }
            product.updateQuantity(quantity); // 변경감지(dirty checking)로 인해 별도의 save 불필요

            OrderDetail orderDetail = OrderDetail.builder()
                    .product(product)
                    .ordering(ordering)
                    .quantity(quantity)
                    .build();
            ordering.getOrderDetails().add(orderDetail);
        }

        Ordering savedOrdering = orderingRepository.save(ordering);
        return savedOrdering;
    }

    public List<OrderListResDto> orderList(){
        List<Ordering> orderings = orderingRepository.findAll();
        List<OrderListResDto> orderListResDtos = new ArrayList<>();
        for (Ordering ordering : orderings){
            orderListResDtos.add(ordering.fromEntity());
        }

        return orderListResDtos;
    }

    public List<OrderListResDto> myOrders(){


        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName(); // 이메일을 꺼내기
        Member member = memberRepository.findByEmail(memberEmail).orElseThrow(() -> new EntityNotFoundException("member is not found"));

        List<Ordering> orderings = orderingRepository.findByMember(member);
        List<OrderListResDto> orderListResDtos = new ArrayList<>();
        for (Ordering ordering : orderings){
            orderListResDtos.add(ordering.fromEntity());
        }

        return orderListResDtos;
    }

    public Ordering orderCancel(Long id){

        Ordering ordering = orderingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("order is not found"));
        ordering.updateStatus(OrderStatus.CANCLED);
        return ordering;

    }

}
