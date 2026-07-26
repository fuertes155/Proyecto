import '../entities/solidarity_group.dart';

abstract class SolidarityRepository {
  Future<List<SolidarityGroup>> getMyGroups();
  Future<SolidarityGroup> createGroup(String name);
  Future<SolidarityGroup> joinGroup(String groupCode);
}
