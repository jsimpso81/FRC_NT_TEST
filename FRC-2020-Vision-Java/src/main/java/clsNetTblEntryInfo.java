// ===============================================================================
// --------to optimize network table I/O
//
//
// ----------------------- revision history --------------------------------------
//
// ===============================================================================


import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

public class clsNetTblEntryInfo {

    private String strName = "";
    private boolean boolEntryFound = false;
    private boolean boolNetTableSet = false;
    private NetworkTableEntry objNTEntry;
    private NetworkTableInstance objNetTbl;

    public clsNetTblEntryInfo( String name ) {
      // --------should check for blank!
      strName = name;
    }

    public void SetTable( NetworkTableInstance inst ) {
      objNetTbl = inst;
      boolNetTableSet = true;
    }

    public void SetEntry( NetworkTableEntry entry ) {
      objNTEntry = entry;
      boolEntryFound = true;
    }

    
    public double GetDouble( double default_value ) {
      double rtnValue = default_value;

      if ( boolNetTableSet) {
        if ( !boolEntryFound ) {
          this.SetEntry( this.objNetTbl.getEntry( this.strName ) );
        }
        rtnValue = this.objNTEntry.getDouble( default_value );
      }
      return rtnValue;
    }


    public void WriteBoolean( boolean value ) {
      if ( boolNetTableSet) {
        if ( !boolEntryFound ) {
          this.SetEntry( this.objNetTbl.getEntry( this.strName ) );
        }
        this.objNTEntry.forceSetBoolean(value);
        // uncertain of difference between set and force
        // debug  System.out.println(this.strName + ": " + value);
      }        
    }

    public void WriteDouble( double value ) {
      if ( boolNetTableSet) {
        if ( !boolEntryFound ) {
          this.SetEntry( this.objNetTbl.getEntry( this.strName ) );
        }
        //this.objNTEntry.forceSetDouble(value);
        this.objNTEntry.setDouble(value);
        // uncertain of difference between set and force
        // debug  System.out.println(this.strName + ": " + value);
      }        
    }
    public void Flush( ) {
      if ( boolNetTableSet) {
        objNetTbl.flush();
      }        
    }


  }   // end cls
