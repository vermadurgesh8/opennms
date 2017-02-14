//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.02.14 at 11:24:05 AM EST 
//


package org.opennms.netmgt.config.prometheus;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
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
 *       &lt;attribute name="filter-exp" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *       &lt;attribute name="alias-exp" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *       &lt;attribute name="type"&gt;
 *         &lt;simpleType&gt;
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *             &lt;pattern value="([Cc](ounter|OUNTER)|[Gg](auge|AUGE))"/&gt;
 *           &lt;/restriction&gt;
 *         &lt;/simpleType&gt;
 *       &lt;/attribute&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "numeric-attribute")
public class NumericAttribute {

    @XmlAttribute(name = "filter-exp")
    @XmlSchemaType(name = "anySimpleType")
    protected String filterExp;
    @XmlAttribute(name = "alias-exp", required = true)
    @XmlSchemaType(name = "anySimpleType")
    protected String aliasExp;
    @XmlAttribute(name = "type")
    protected String type;

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
     * Gets the value of the aliasExp property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAliasExp() {
        return aliasExp;
    }

    /**
     * Sets the value of the aliasExp property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAliasExp(String value) {
        this.aliasExp = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof NumericAttribute)) {
            return false;
        }
        NumericAttribute castOther = (NumericAttribute) other;
        return Objects.equals(filterExp, castOther.filterExp) && Objects.equals(aliasExp, castOther.aliasExp)
                && Objects.equals(type, castOther.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filterExp, aliasExp, type);
    }

}
