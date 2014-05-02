package dynamake;

/*

Instances represent collapsable/expandable models. 
I.e. they can be expanded (pulled out) if they are collapsed (drawn in) - or they can be collapsed (pushed in) if they are expanded (drawn out).
- Like a drawer

May practical and economical in terms of screen real estate in certain situations
May only contains a single model
- In direct terms, which means they may contain canvases, which themselves may contain multiple models.



A drawer's contained item is always filled out on both dimensions?

Should perhaps contain two models: one for the collapsed view and one for the expanded view?

Or, it should simply collapse to a certain height (and width/maximum width)

Or, simply, it should contain two bounds: 
- one for the collapsed state (which is restricted in some ways, for instance in height and maximum width - perhaps relative to the other bounds)
- one for the expanded state
The respective bounds are then applied for the respective state.

*/
public class DrawerModel {

}
