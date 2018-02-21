//
// --------------------------------------------------------------------------
//  Gurux Ltd
// 
//
//
// Filename:        $HeadURL$
//
// Version:         $Revision$,
//                  $Date$
//                  $Author$
//
// Copyright (c) Gurux Ltd
//
//---------------------------------------------------------------------------
//
//  DESCRIPTION
//
// This file is a part of Gurux Device Framework.
//
// Gurux Device Framework is Open Source software; you can redistribute it
// and/or modify it under the terms of the GNU General Public License 
// as published by the Free Software Foundation; version 2 of the License.
// Gurux Device Framework is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of 
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
// See the GNU General Public License for more details.
//
// More information of Gurux products: http://www.gurux.org
//
// This code is licensed under the GNU General Public License v2. 
// Full text may be retrieved at http://www.gnu.org/licenses/gpl-2.0.txt
//---------------------------------------------------------------------------

package com.aimir.fep.bypass.dlms.objects;

import com.aimir.fep.bypass.dlms.enums.ObjectType;

public class DLMSObjectDefinition 
{
    ObjectType ClassId;
    String LogicalName;

    public final ObjectType getClassId()
    {
        return ClassId;
    }
    
    public final void setClassId(ObjectType value)
    {
        ClassId = value;
    }

    public final String getLogicalName()
    {
        return LogicalName;
    }
    public final void setLogicalName(String value)
    {
        LogicalName = value;
    }

    /*
     * Constructor
     */
    public DLMSObjectDefinition()
    {

    }

    /*
     * Constructor
     */
    public DLMSObjectDefinition(ObjectType classId, String logicalName)
    {
        ClassId = classId;
        LogicalName = logicalName;
    }   
    
    @Override 
    public String toString()
    {
        return ClassId.toString() + " " + LogicalName;
    }
}
