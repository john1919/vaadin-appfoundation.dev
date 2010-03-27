package org.vaadin.appfoundation.authorization.jpa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vaadin.appfoundation.authorization.PermissionManager;
import org.vaadin.appfoundation.authorization.Resource;
import org.vaadin.appfoundation.authorization.Role;
import org.vaadin.appfoundation.persistence.facade.FacadeFactory;

/**
 * JPA based implementation for the {@link PermissionManager} interface. This
 * class will communicate directly with the database, where it will store all
 * defined permissions.
 * 
 * @author Kim
 * 
 */
public class JPAPermissionManager implements PermissionManager {

    /**
     * {@inheritDoc}
     */
    public void allow(Role role, String action, Resource resource) {
        checkRoleAndResourceNotNull(role, resource);
        Map<PermissionType, PermissionEntity> permissions = getPermissions(
                role, action, resource);

        if (permissions.containsKey(PermissionType.DENY)) {
            PermissionEntity permission = permissions.get(PermissionType.DENY);
            permission.setType(PermissionType.ALLOW);
            FacadeFactory.getFacade().store(permission);
        } else {
            createPermissionEntity(PermissionType.ALLOW, role, action, resource);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void allowAll(Role role, Resource resource) {
        checkRoleAndResourceNotNull(role, resource);
        Map<PermissionType, PermissionEntity> permissions = getPermissions(
                role, null, resource);

        if (permissions.containsKey(PermissionType.DENY_ALL)) {
            PermissionEntity permission = permissions
                    .get(PermissionType.DENY_ALL);
            permission.setType(PermissionType.ALLOW_ALL);
            FacadeFactory.getFacade().store(permission);
        } else {
            createPermissionEntity(PermissionType.ALLOW_ALL, role, null,
                    resource);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deny(Role role, String action, Resource resource) {
        checkRoleAndResourceNotNull(role, resource);
        Map<PermissionType, PermissionEntity> permissions = getPermissions(
                role, action, resource);

        if (permissions.containsKey(PermissionType.ALLOW)) {
            PermissionEntity permission = permissions.get(PermissionType.ALLOW);
            permission.setType(PermissionType.DENY);
            FacadeFactory.getFacade().store(permission);
        } else {
            createPermissionEntity(PermissionType.DENY, role, action, resource);
        }

    }

    /**
     * {@inheritDoc}
     */
    public void denyAll(Role role, Resource resource) {
        checkRoleAndResourceNotNull(role, resource);
        Map<PermissionType, PermissionEntity> permissions = getPermissions(
                role, null, resource);

        if (permissions.containsKey(PermissionType.ALLOW_ALL)) {
            PermissionEntity permission = permissions
                    .get(PermissionType.ALLOW_ALL);
            permission.setType(PermissionType.DENY_ALL);
            FacadeFactory.getFacade().store(permission);
        } else {
            createPermissionEntity(PermissionType.DENY_ALL, role, null,
                    resource);
        }

    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAccess(Role role, String action, Resource resource) {
        checkRoleAndResourceNotNull(role, resource);

        Map<PermissionType, PermissionEntity> permissions = getPermissions(
                role, action, resource);

        if (permissions.containsKey(PermissionType.ALLOW)) {
            return true;
        }

        if (permissions.containsKey(PermissionType.DENY)) {
            return false;
        }

        if (permissions.containsKey(PermissionType.ALLOW_ALL)) {
            return true;
        }

        if (permissions.containsKey(PermissionType.DENY_ALL)) {
            return false;
        }

        if (hasResourceActionAllowPermissions(resource, action)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the given resource-action combination has any allow or allow
     * all permissions set for any role
     * 
     * @param resource
     * @param action
     * @return
     */
    private boolean hasResourceActionAllowPermissions(Resource resource,
            String action) {
        String whereClause = "p.resource = :resource AND ("
                + "(p.action = :action AND p.type = :typeAllow) OR "
                + "(p.type = :typeAllowAll))";
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("resource", resource.getIdentifier());
        parameters.put("action", action);
        parameters.put("typeAllow", PermissionType.ALLOW);
        parameters.put("typeAllowAll", PermissionType.ALLOW_ALL);

        Long count = FacadeFactory.getFacade().count(PermissionEntity.class,
                whereClause, parameters);

        return count > 0 ? true : false;
    }

    /**
     * Checks that neither role or resource is null.
     * 
     * @param role
     * @param resource
     * @throws IllegalArgumentException
     *             Thrown if either role or resource is null
     */
    private void checkRoleAndResourceNotNull(Role role, Resource resource) {
        if (role == null) {
            throw new IllegalArgumentException("Role may not be null");
        }

        if (resource == null) {
            throw new IllegalArgumentException("Role may not be null");
        }
    }

    /**
     * Creates a PermissionEntity for the given details and persists it.
     * 
     * @param type
     * @param role
     * @param action
     * @param resource
     * @return
     */
    private void createPermissionEntity(PermissionType type, Role role,
            String action, Resource resource) {
        PermissionEntity permission = new PermissionEntity(type);
        permission.setRole(role.getIdentifier());
        permission.setAction(action);
        permission.setResource(resource.getIdentifier());
        FacadeFactory.getFacade().store(permission);
    }

    /**
     * Fetches the permission entities for the given role-action-resource
     * combination.
     * 
     * @param role
     * @param action
     * @param resource
     * @return A map between the {@link PermissionType} and
     *         {@link PermissionEntity} of all the entities found with the given
     *         parameter combination
     */
    private Map<PermissionType, PermissionEntity> getPermissions(Role role,
            String action, Resource resource) {
        String queryStr = "SELECT p FROM PermissionEntity p WHERE p.role = :role "
                + "AND p.resource = :resource AND ((p.action = :action "
                + "AND (p.type = :typeAllow OR p.type = :typeDeny)) "
                + "OR (p.type = :typeAllowAll OR p.type = :typeDenyAll))";
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("role", role.getIdentifier());
        parameters.put("action", action);
        parameters.put("resource", resource.getIdentifier());
        parameters.put("typeAllow", PermissionType.ALLOW);
        parameters.put("typeDeny", PermissionType.DENY);
        parameters.put("typeAllowAll", PermissionType.ALLOW_ALL);
        parameters.put("typeDenyAll", PermissionType.DENY_ALL);

        List<PermissionEntity> entities = FacadeFactory.getFacade().list(
                queryStr, parameters);

        Map<PermissionType, PermissionEntity> map = new HashMap<PermissionType, PermissionEntity>();
        if (entities != null) {
            for (PermissionEntity entity : entities) {
                map.put(entity.getType(), entity);
            }
        }

        return map;
    }
}