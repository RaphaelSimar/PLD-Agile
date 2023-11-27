module fr.insalyon.heptabits.pldagile {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.xml;

    opens fr.insalyon.heptabits.pldagile to javafx.fxml;
    exports fr.insalyon.heptabits.pldagile;
    exports fr.insalyon.heptabits.pldagile.controller;
    exports fr.insalyon.heptabits.pldagile.model;
    opens fr.insalyon.heptabits.pldagile.controller to javafx.fxml;


}