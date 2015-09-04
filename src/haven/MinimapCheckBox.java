package haven;

public class MinimapCheckBox extends CheckBox{
    String name;
    public MinimapCheckBox(String lbl) {
        super(lbl);
        this.name = lbl;
        if (MinimapIcons.ToggledIcons.contains(lbl)) {
            this.set(true);
        }
    }
    public boolean CheckBoxStatus(){
        return this.a;
    }
}
