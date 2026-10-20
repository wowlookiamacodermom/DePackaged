package com.example.madprojectnamedepackaged;

/**
 * One packaged product, loaded from assets/products.csv.
 * All nutrient values are per 100 g (or per 100 ml for beverages).
 */
public class Product {

    public String barcode;
    public String name;
    public String brand;
    public String company;
    public boolean veg;
    public String category;
    public double packGrams;

    public double calories;
    public double sugarG;
    public double sodiumMg;
    public double satFatG;
    public double fatG;
    public double proteinG;

    public String imageName;          // drawable resource name, may be empty

    public String naturalAlt;         // fruit / vegetable / whole-food swap
    public String naturalWhy;
    public String naturalImage;       // drawable resource name, may be empty

    public String packagedAltBarcode; // barcode of a better packaged product
    public String packagedWhy;

    /** Multiplier to turn "per 100 g" values into "per pack" values. */
    public double factor(boolean perPack) {
        return (perPack && packGrams > 0) ? packGrams / 100.0 : 1.0;
    }

    public boolean isBeverage() {
        return category != null && category.equalsIgnoreCase("Beverage");
    }
}
