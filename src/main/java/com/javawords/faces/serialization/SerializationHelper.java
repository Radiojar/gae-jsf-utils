package com.javawords.faces.serialization;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

/**
 *
 * @author Christos Fragoulides
 */
public class SerializationHelper {
    
    public static void writeDataModel(DataModel<?> dataModel, ObjectOutputStream out) throws IOException {
        if (dataModel != null) {
            out.writeBoolean(true);
            out.writeObject(dataModel.getWrappedData());
            out.writeInt(dataModel.getRowIndex());
        }
        else out.writeBoolean(false);
    }
    
    public static <E> DataModel<E> readDataModel(ObjectInputStream in) 
            throws IOException, ClassNotFoundException {
        
        DataModel<E> result = null;
        if (in.readBoolean()) {
            result = new ListDataModel<E>();
            result.setWrappedData(in.readObject());
            result.setRowIndex(in.readInt());            
        }            
        return result;   
    }
    
}
