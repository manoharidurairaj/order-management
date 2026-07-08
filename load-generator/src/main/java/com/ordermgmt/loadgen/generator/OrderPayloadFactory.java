package com.ordermgmt.loadgen.generator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class OrderPayloadFactory {

    private static final List<MenuItem> MENU = List.of(
            new MenuItem("sku-pizza-margherita", "Margherita Pizza", BigDecimal.valueOf(11.50)),
            new MenuItem("sku-pizza-pepperoni", "Pepperoni Pizza", BigDecimal.valueOf(13.00)),
            new MenuItem("sku-burger-classic", "Classic Burger", BigDecimal.valueOf(9.25)),
            new MenuItem("sku-burger-veggie", "Veggie Burger", BigDecimal.valueOf(9.75)),
            new MenuItem("sku-sushi-roll", "California Roll", BigDecimal.valueOf(8.50)),
            new MenuItem("sku-noodles-pad-thai", "Pad Thai", BigDecimal.valueOf(10.00)),
            new MenuItem("sku-salad-caesar", "Caesar Salad", BigDecimal.valueOf(7.25)),
            new MenuItem("sku-drink-soda", "Soda", BigDecimal.valueOf(2.50)),
            new MenuItem("sku-dessert-brownie", "Brownie", BigDecimal.valueOf(4.00))
    );

    public GeneratedOrder randomOrder() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String customerId = "customer-" + random.nextInt(1, 5000);

        int itemCount = random.nextInt(1, 4);
        List<GeneratedOrder.Item> items = random.ints(itemCount, 0, MENU.size())
                .mapToObj(MENU::get)
                .map(menuItem -> new GeneratedOrder.Item(
                        menuItem.productId(), menuItem.name(), random.nextInt(1, 4), menuItem.unitPrice()))
                .toList();

        return new GeneratedOrder(customerId, items);
    }
}
