package studentmanage;

import java.util.Objects;

/**
 * 作者：cyt
 * 功能：封装学生地址信息。
 * 编写时间：2026-05-05
 */
public class Address {
    /** 省份信息。作者：cyt；编写时间：2026-05-05 */
    private String province;
    /** 城市信息。作者：cyt；编写时间：2026-05-05 */
    private String city;
    /** 街道信息。作者：cyt；编写时间：2026-05-05 */
    private String street;
    /** 门牌号信息。作者：cyt；编写时间：2026-05-05 */
    private String houseNumber;

    /**
     * 作者：cyt
     * 功能：创建地址对象。
     * 编写时间：2026-05-05
     * @param province 省份
     * @param city 城市
     * @param street 街道
     * @param houseNumber 门牌号
     */
    public Address(String province, String city, String street, String houseNumber) {
        this.province = province;
        this.city = city;
        this.street = street;
        this.houseNumber = houseNumber;
    }

    /**
     * 作者：cyt
     * 功能：获取省份。
     * 编写时间：2026-05-05
     * @return 省份
     */
    public String getProvince() {
        return province;
    }

    /**
     * 作者：cyt
     * 功能：设置省份。
     * 编写时间：2026-05-05
     * @param province 省份
     */
    public void setProvince(String province) {
        this.province = province;
    }

    /**
     * 作者：cyt
     * 功能：获取城市。
     * 编写时间：2026-05-05
     * @return 城市
     */
    public String getCity() {
        return city;
    }

    /**
     * 作者：cyt
     * 功能：设置城市。
     * 编写时间：2026-05-05
     * @param city 城市
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * 作者：cyt
     * 功能：获取街道。
     * 编写时间：2026-05-05
     * @return 街道
     */
    public String getStreet() {
        return street;
    }

    /**
     * 作者：cyt
     * 功能：设置街道。
     * 编写时间：2026-05-05
     * @param street 街道
     */
    public void setStreet(String street) {
        this.street = street;
    }

    /**
     * 作者：cyt
     * 功能：获取门牌号。
     * 编写时间：2026-05-05
     * @return 门牌号
     */
    public String getHouseNumber() {
        return houseNumber;
    }

    /**
     * 作者：cyt
     * 功能：设置门牌号。
     * 编写时间：2026-05-05
     * @param houseNumber 门牌号
     */
    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    /**
     * 作者：cyt
     * 功能：格式化地址字符串。
     * 编写时间：2026-05-05
     * @return 地址文本
     */
    @Override
    public String toString() {
        return String.format("%s, %s, %s, %s", province, city, street, houseNumber);
    }

    /**
     * 作者：cyt
     * 功能：地址相等性比较。
     * 编写时间：2026-05-05
     * @param o 对比对象
     * @return 是否相等
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Address address = (Address) o;
        return Objects.equals(province, address.province)
                && Objects.equals(city, address.city)
                && Objects.equals(street, address.street)
                && Objects.equals(houseNumber, address.houseNumber);
    }

    /**
     * 作者：cyt
     * 功能：生成地址哈希值。
     * 编写时间：2026-05-05
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(province, city, street, houseNumber);
    }
}
