package stream.api;

import common.test.tool.annotation.Difficult;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.entity.Item;
import common.test.tool.entity.Shop;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class Exercise8Test extends ClassicOnlineStore {

    @Difficult
    @Test
    public void itemsNotOnSale() {
        Stream<Customer> customerStream = this.mall.getCustomerList().stream();
        Stream<Shop> shopStream = this.mall.getShopList().stream();

        /**
         * Create a set of item names that are in {@link Customer.wantToBuy} but not on sale in any shop.
         */
        List<String> itemListOnSale = shopStream
                .flatMap(s -> s.getItemList().stream())
                .map(Item::getName)
                .collect(Collectors.toList());
        Set<String> itemSetNotOnSale = customerStream
                .flatMap(c -> c.getWantToBuy().stream())
                .map(Item::getName)
                .filter(name -> !itemListOnSale.contains(name))
                .collect(Collectors.toSet());

        assertThat(itemSetNotOnSale, hasSize(3));
        assertThat(itemSetNotOnSale, hasItems("bag", "pants", "coat"));
    }

    @Difficult
    @Test
    public void havingEnoughMoney() {
        Stream<Customer> customerStream = this.mall.getCustomerList().stream();
        Stream<Shop> shopStream = this.mall.getShopList().stream();

        /**
         * Create a customer's name list including who are having enough money to buy all items they want which is on sale.
         * Items that are not on sale can be counted as 0 money cost.
         * If there is several same items with different prices, customer can choose the cheapest one.
         */
        List<Item> onSaleItems = shopStream
                .flatMap(s -> s.getItemList().stream())
                .collect(Collectors.toList());

        Function<Item, Integer> getCheapestItemPriceWithSameName = (item) -> {
            List<Item> itemsWithSameName = onSaleItems.stream()
                    .filter(i -> i.getName().equals(item.getName()))
                    .collect(Collectors.toList());

            Item cheapestItem = itemsWithSameName.stream()
                    .min(Comparator.comparing(Item::getPrice))
                    .orElse(null);

            if (cheapestItem == null) {
                return 0;
            }
            return cheapestItem.getPrice();
        };

        Predicate<Customer> havingEnoughMoney = c -> {
            Integer neededMoney = c.getWantToBuy().stream()
                    .map(getCheapestItemPriceWithSameName)
                    .mapToInt(i -> i)
                    .sum();
            return c.getBudget() >= neededMoney;
        };

        List<String> customerNameList = customerStream
                .filter(havingEnoughMoney)
                .map(Customer::getName)
                .collect(Collectors.toList());

        assertThat(customerNameList, hasSize(7));
        assertThat(customerNameList, hasItems("Joe", "Patrick", "Chris", "Kathy", "Alice", "Andrew", "Amy"));
    }
}
