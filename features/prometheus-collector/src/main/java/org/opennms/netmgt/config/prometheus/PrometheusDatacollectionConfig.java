//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.02.14 at 11:24:05 AM EST 
//


package org.opennms.netmgt.config.prometheus;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{http://xmlns.opennms.org/xsd/config/prometheus-datacollection}collection" maxOccurs="unbounded"/&gt;
 *         &lt;element ref="{http://xmlns.opennms.org/xsd/config/prometheus-datacollection}group" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="rrdRepository" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "collection",
    "group"
})
@XmlRootElement(name = "prometheus-datacollection-config")
public class PrometheusDatacollectionConfig {

    @XmlElement(required = true)
    protected List<Collection> collection;
    @XmlElement(required = true)
    protected List<Group> group;
    @XmlAttribute(name = "rrd-repository")
    protected String rrdRepository;

    /**
     * Gets the value of the collection property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the collection property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCollection().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Collection }
     * 
     * 
     */
    public List<Collection> getCollection() {
        if (collection == null) {
            collection = new ArrayList<Collection>();
        }
        return this.collection;
    }

    /**
     * Gets the value of the group property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the group property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Group }
     * 
     * 
     */
    public List<Group> getGroup() {
        if (group == null) {
            group = new ArrayList<Group>();
        }
        return this.group;
    }

    /**
     * Gets the value of the rrdRepository property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRrdRepository() {
        return rrdRepository;
    }

    /**
     * Sets the value of the rrdRepository property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRrdRepository(String value) {
        this.rrdRepository = value;
    }

    public PrometheusDatacollectionConfig merge(PrometheusDatacollectionConfig other) {
        if (other == null) {
            return this;
        }
        // Overwrite the rrdRepository iff it's null
        if (rrdRepository == null && other.rrdRepository != null) {
            rrdRepository = other.rrdRepository;
        }
        // Merge the lists of collections and groups
        getCollection().addAll(other.getCollection());
        getGroup().addAll(other.getGroup());
        return this;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof PrometheusDatacollectionConfig)) {
            return false;
        }
        PrometheusDatacollectionConfig castOther = (PrometheusDatacollectionConfig) other;
        return Objects.equals(collection, castOther.collection) && Objects.equals(group, castOther.group)
                && Objects.equals(rrdRepository, castOther.rrdRepository);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collection, group, rrdRepository);
    }

}
