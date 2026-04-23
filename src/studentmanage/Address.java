package studentmanage;

import java.util.Objects;

public class Address {
    private String province;
    private String city;
    private String street;
    private String houseNumber;

    public Address(String province, String city, String street, String houseNumber) {
        this.province = province;
        this.city = city;
        this.street = street;
        this.houseNumber = houseNumber;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    @Override
    public String toString() {
        return String.format("%s, %s, %s, %s", province, city, street, houseNumber);
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(province, city, street, houseNumber);
    }
}

