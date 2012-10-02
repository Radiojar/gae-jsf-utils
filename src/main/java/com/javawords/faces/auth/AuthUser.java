/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.javawords.faces.auth;

import java.io.Serializable;

/**
 *
 * @author cfragoulidis
 */
public interface AuthUser extends Serializable {
    public boolean isAdmin();
}
