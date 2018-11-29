package edu.tamu.di.SAFCreator.model;

import java.util.ArrayList;
import java.util.List;

public class Bundle {
    private String name;
    private Item item;
    private List<Bitstream> bitstreams = new ArrayList<Bitstream>();

    public void addBitstream(Bitstream bitstream) {
        bitstreams.add(bitstream);

    }

    public List<Bitstream> getBitstreams() {
        return bitstreams;
    }

    public Item getItem() {
        return item;
    }

    public String getName() {
        return name;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void setName(String name) {
        this.name = name;
    }
}
