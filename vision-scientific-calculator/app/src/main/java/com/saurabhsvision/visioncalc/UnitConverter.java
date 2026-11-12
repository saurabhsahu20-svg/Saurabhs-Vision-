package com.saurabhsvision.visioncalc;

/**
 * Offline dimensional conversions.
 * Coefficients convert one selected unit into the category base unit:
 * Length->metre, Mass->kilogram, Area->square metre, Volume->litre,
 * Time->second, Speed->m/s, Pressure->pascal, Energy->joule,
 * Angle->radian, Data->byte and Frequency->hertz.
 */
public final class UnitConverter {
    private UnitConverter(){}
    public static final class Category {
        public final String name;
        public final String[] units;
        public final double[] toBase;
        Category(String name,String[] units,double[] toBase){
            this.name=name;this.units=units;this.toBase=toBase;
            if(units.length!=toBase.length)throw new IllegalArgumentException("units mismatch");
        }
    }
    private static Category category(String name,String[] units,double... factors){
        return new Category(name,units,factors);
    }
    private static final Category[] CATEGORIES={
        category("Length",new String[]{"mm","cm","m","km","inch","ft","yard","mile","nautical mile"},
            .001,.01,1,1000,.0254,.3048,.9144,1609.344,1852),
        category("Mass",new String[]{"mg","g","kg","tonne","oz","lb","stone"},
            .000001,.001,1,1000,.028349523125,.45359237,6.35029318),
        category("Temperature",new String[]{"°C","°F","K"},1,1,1),
        category("Area",new String[]{"mm²","cm²","m²","km²","ft²","acre","hectare"},
            .000001,.0001,1,1000000,.09290304,4046.8564224,10000),
        category("Volume",new String[]{"mL","L","m³","US fl oz","US cup","US gallon"},
            .001,1,1000,.0295735295625,.2365882365,3.785411784),
        category("Time",new String[]{"ms","s","min","hour","day","week","year (365 days)"},
            .001,1,60,3600,86400,604800,31536000),
        category("Speed",new String[]{"m/s","km/h","mph","knot","ft/s"},
            1,1.0/3.6,.44704,1852.0/3600,.3048),
        category("Pressure",new String[]{"Pa","kPa","bar","atm","psi","mmHg"},
            1,1000,100000,101325,6894.757293168,133.322387415),
        category("Energy",new String[]{"J","kJ","cal (thermochemical)","kcal","Wh","kWh","eV"},
            1,1000,4.184,4184,3600,3600000,1.602176634e-19),
        category("Angle",new String[]{"degree","radian","gradian","turn"},
            Math.PI/180,1,Math.PI/200,2*Math.PI),
        category("Data",new String[]{"bit","byte","KB (1000)","MB (1000²)","GB (1000³)","KiB (1024)","MiB (1024²)","GiB (1024³)"},
            .125,1,1000,1e6,1e9,1024,1048576,1073741824),
        category("Frequency",new String[]{"Hz","kHz","MHz","GHz","rpm"},
            1,1000,1e6,1e9,1.0/60)
    };
    public static int count(){return CATEGORIES.length;}
    public static Category get(int i){
        if(i<0||i>=CATEGORIES.length)throw new IllegalArgumentException("Invalid category");
        return CATEGORIES[i];
    }
    public static String[] names(){
        String[] result=new String[CATEGORIES.length];
        for(int i=0;i<result.length;i++)result[i]=CATEGORIES[i].name;
        return result;
    }
    public static double convert(double value,int category,int from,int to){
        Category data=get(category);
        if(from<0||to<0||from>=data.units.length||to>=data.units.length)
            throw new IllegalArgumentException("Invalid unit");
        if(!Double.isFinite(value))throw new IllegalArgumentException("Invalid value");
        if(category==2){ // Temperature is affine, not multiplicative.
            double kelvin;
            switch(from){
                case 0:kelvin=value+273.15;break;
                case 1:kelvin=(value-32)*5/9+273.15;break;
                default:kelvin=value;
            }
            // Below absolute zero is not a physical temperature.
            if(kelvin < -1e-9)throw new IllegalArgumentException("Temperature below absolute zero");
            if(kelvin < 0)kelvin=0;
            switch(to){
                case 0:return kelvin-273.15;
                case 1:return (kelvin-273.15)*9/5+32;
                default:return kelvin;
            }
        }
        double answer=(value*data.toBase[from])/data.toBase[to];
        if(!Double.isFinite(answer))throw new IllegalArgumentException("Value too large");
        return answer;
    }
}
