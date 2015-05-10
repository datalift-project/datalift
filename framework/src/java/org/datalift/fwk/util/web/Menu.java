package org.datalift.fwk.util.web;


import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;


public class Menu implements Collection<MenuEntry>
{
    private final Collection<MenuEntry> entries = new TreeSet<MenuEntry>();

    public Menu() {
        super();
    }

    @Override
    public boolean add(MenuEntry e) {
        return this.entries.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends MenuEntry> c) {
        return this.entries.addAll(c);
    }

    @Override
    public boolean remove(Object o) {
        return this.entries.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return this.entries.removeAll(c);
    }

    @Override
    public void clear() {
        this.entries.clear();
    }

    @Override
    public int size() {
        return this.entries.size();
    }

    @Override
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.entries.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return this.entries.containsAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this.entries.retainAll(c);
    }

    @Override
    public Iterator<MenuEntry> iterator() {
        return this.entries.iterator();
    }

    @Override
    public Object[] toArray() {
        return this.entries.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return this.entries.toArray(a);
    }
}
