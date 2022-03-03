package stream.api;

import common.test.tool.annotation.Difficult;
import common.test.tool.annotation.Easy;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.entity.Item;
import common.test.tool.util.CollectorImpl;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class Exercise9Test extends ClassicOnlineStore {

    @Easy
    @Test
    public void simplestStringJoin() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a String with comma separated names shown in the assertion.
         * The collector will be used by serial stream.
         */
        Supplier<StringBuilder> supplier = StringBuilder::new;

        BiConsumer<StringBuilder, String> accumulator = (stringBuilder, str) ->
                stringBuilder.append(str).append(",");

        BinaryOperator<StringBuilder> combiner = null;

        Function<StringBuilder, String> finisher = (builder) -> {
            builder.deleteCharAt(builder.length() - 1);
            return builder.toString();
        };

        Collector<String, ?, String> toCsv =
                new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());
        String nameAsCsv = customerList.stream().map(Customer::getName).collect(toCsv);
        assertThat(nameAsCsv, is("Joe,Steven,Patrick,Diana,Chris,Kathy,Alice,Andrew,Martin,Amy"));
    }

    @Difficult
    @Test
    public void mapKeyedByItems() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a {@link Map} with keys as item and
         * values as {@link Set} of customers who are wanting to buy that item.
         * The collector will be used by parallel stream.
         */
        Supplier<Map<String, Set<String>>> supplier = ConcurrentHashMap::new;

        BiConsumer<Map<String, Set<String>>, Customer> accumulator = (map, customer) -> {
            for (Item item : customer.getWantToBuy()) {
                Set<String> itemCustomers = map.getOrDefault(item.getName(), new HashSet<>());
                itemCustomers.add(customer.getName());
                map.put(item.getName(), itemCustomers);
            }
        };

        BinaryOperator<Map<String, Set<String>>> combiner = (map1, map2) -> {
            map2.forEach((map2Item, map2Customers) -> {
                Set<String> map1Customers = map1.getOrDefault(map2Item, new HashSet<>());
                map2Customers.addAll(map1Customers);
                map1.put(map2Item, map2Customers);
            });
            return map1;
        };

        Function<Map<String, Set<String>>, Map<String, Set<String>>> finisher = null;

        Collector<Customer, ?, Map<String, Set<String>>> toItemAsKey =
                new CollectorImpl<>(supplier, accumulator, combiner, finisher, EnumSet.of(
                        Collector.Characteristics.CONCURRENT,
                        Collector.Characteristics.IDENTITY_FINISH));
        Map<String, Set<String>> itemMap = customerList.stream().parallel().collect(toItemAsKey);
        assertThat(itemMap.get("plane"), containsInAnyOrder("Chris"));
        assertThat(itemMap.get("onion"), containsInAnyOrder("Patrick", "Amy"));
        assertThat(itemMap.get("ice cream"), containsInAnyOrder("Patrick", "Steven"));
        assertThat(itemMap.get("earphone"), containsInAnyOrder("Steven"));
        assertThat(itemMap.get("plate"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("fork"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("cable"), containsInAnyOrder("Diana", "Steven"));
        assertThat(itemMap.get("desk"), containsInAnyOrder("Alice"));
    }

    @Difficult
    @Test
    public void bitList2BitString() {
        String bitList = "22-24,9,42-44,11,4,46,14-17,5,2,38-40,33,50,48";

        /**
         * Create a {@link String} of "n"th bit ON.
         * for example
         * "3" will be "001"
         * "1,3,5" will be "10101"
         * "1-3" will be "111"
         * "7,1-3,5" will be "1110101"
         */

        Supplier<List<Integer>> supplier = ArrayList::new;

        BiConsumer<List<Integer>, String> accumulator = (list, bitListElement) -> {
            if (bitListElement.contains("-")) {
                String[] limits = bitListElement.split("-");
                int iFrom = Integer.parseInt(limits[0]);
                int iTo = Integer.parseInt(limits[1]);
                list.addAll(Collections.nCopies(Math.max(0, iTo - list.size()), 0));
                for (int i = iFrom; i <= iTo; i++) {
                    list.set(i - 1, 1);
                }
            } else {
                int newInt = Integer.parseInt(bitListElement);
                list.addAll(Collections.nCopies(Math.max(0, newInt - list.size()), 0));
                list.set(newInt - 1, 1);
            }
        };

        BinaryOperator<List<Integer>> combiner = null;

        Function<List<Integer>, String> finisher = (list) -> {
            StringBuilder builder = new StringBuilder();
            for (Integer i : list) {
                builder.append(i);
            }
            return builder.toString();
        };

        Collector<String, ?, String> toBitString = new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());

        String bitString = Arrays.stream(bitList.split(",")).collect(toBitString);
        assertThat(bitString, is("01011000101001111000011100000000100001110111010101")

        );
    }
}
