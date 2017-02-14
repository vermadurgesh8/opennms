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
 *         &lt;element ref="{http://xmlns.opennms.org/xsd/config/prometheus-datacollection}numeric-attribute" maxOccurs="unbounded"/&gt;
 *         &lt;element ref="{http://xmlns.opennms.org/xsd/config/prometheus-datacollection}string-attribute" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="resource-type" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="filter-exp" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="group-by-exp" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "numericAttribute",
    "stringAttribute"
})
@XmlRootElement(name = "group")
public class Group {

    @XmlElement(name = "numeric-attribute", required = true)
    protected List<NumericAttribute> numericAttribute;
    @XmlElement(name = "string-attribute")
    protected List<StringAttribute> stringAttribute;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "resource-type", required = true)
    protected String resourceType;
    @XmlAttribute(name = "filter-exp", required = true)
    protected String filterExp;
    @XmlAttribute(name = "group-by-exp")
    protected String groupByExp;

    /**
     * Gets the value of the numericAttribute property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the numericAttribute property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNumericAttribute().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link NumericAttribute }
     * 
     * 
     */
    public List<NumericAttribute> getNumericAttribute() {
        if (numericAttribute == null) {
            numericAttribute = new ArrayList<NumericAttribute>();
        }
        return this.numericAttribute;
    }

    /**
     * Gets the value of the stringAttribute property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the stringAttribute property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getStringAttribute().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link StringAttribute }
     * 
     * 
     */
    public List<StringAttribute> getStringAttribute() {
        if (stringAttribute == null) {
            stringAttribute = new ArrayList<StringAttribute>();
        }
        return this.stringAttribute;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the resourceType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Sets the value of the resourceType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResourceType(String value) {
        this.resourceType = value;
    }

    /**
     * Gets the value of the filterExp property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFilterExp() {
        return filterExp;
    }

    /**
     * Sets the value of the filterExp property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFilterExp(String value) {
        this.filterExp = value;
    }

    /**
     * Gets the value of the groupByExp property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGroupByExp() {
        return groupByExp;
    }

    /**
     * Sets the value of the groupByExp property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGroupByExp(String value) {
        this.groupByExp = value;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Group)) {
            return false;
        }
        Group castOther = (Group) other;
        return Objects.equals(numericAttribute, castOther.numericAttribute)
                && Objects.equals(stringAttribute, castOther.stringAttribute) && Objects.equals(name, castOther.name)
                && Objects.equals(resourceType, castOther.resourceType)
                && Objects.equals(filterExp, castOther.filterExp) && Objects.equals(groupByExp, castOther.groupByExp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numericAttribute, stringAttribute, name, resourceType, filterExp, groupByExp);
    }

}
